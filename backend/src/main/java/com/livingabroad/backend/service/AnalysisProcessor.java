package com.livingabroad.backend.service;

import com.livingabroad.backend.client.AiServerClient;
import com.livingabroad.backend.dto.ai.AiRecommendRequestDto;
import com.livingabroad.backend.dto.ai.AiRecommendResponseDto;
import com.livingabroad.backend.dto.analysis.AnalysisCreateRequest;
import com.livingabroad.backend.entity.Analysis;
import com.livingabroad.backend.entity.AnalysisCountryResult;
import com.livingabroad.backend.entity.AnalysisResultReason;
import com.livingabroad.backend.entity.VisaProgram;
import com.livingabroad.backend.event.AnalysisRequestedEvent;
import com.livingabroad.backend.repository.AnalysisCountryResultRepository;
import com.livingabroad.backend.repository.AnalysisRepository;
import com.livingabroad.backend.repository.AnalysisResultReasonRepository;
import com.livingabroad.backend.repository.VisaProgramRepository;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// AnalysisService.createAnalysis()는 저장만 하고 즉시 202로 응답하며, 실제 AI 호출은
// 트랜잭션 커밋 이후(AFTER_COMMIT) 이 빈에서 비동기로 처리한다. 같은 서비스 안에서 스스로
// @Async 메서드를 호출하면 프록시를 못 타서 무시되므로(self-invocation) 별도 빈으로 분리했다.
@Service
public class AnalysisProcessor {

    private static final Logger log = LoggerFactory.getLogger(AnalysisProcessor.class);
    private static final List<String> SUPPORTED_COUNTRIES = List.of("CAN", "AUS", "GBR");

    private final AnalysisRepository analysisRepository;
    private final AnalysisCountryResultRepository resultRepository;
    private final AnalysisResultReasonRepository reasonRepository;
    private final VisaProgramRepository visaProgramRepository;
    private final AiServerClient aiServerClient;
    private final SlackNotifier slackNotifier;

    public AnalysisProcessor(
        AnalysisRepository analysisRepository,
        AnalysisCountryResultRepository resultRepository,
        AnalysisResultReasonRepository reasonRepository,
        VisaProgramRepository visaProgramRepository,
        AiServerClient aiServerClient,
        SlackNotifier slackNotifier
    ) {
        this.analysisRepository = analysisRepository;
        this.resultRepository = resultRepository;
        this.reasonRepository = reasonRepository;
        this.visaProgramRepository = visaProgramRepository;
        this.aiServerClient = aiServerClient;
        this.slackNotifier = slackNotifier;
    }

    // @Transactional은 여기(외부에서 실제로 호출되는 진입점)에 있어야 한다. onAnalysisRequested가
    // process(...)를 이 클래스 안에서 직접(this.process(...)) 호출하는데, Spring AOP는 프록시 기반이라
    // 같은 빈 안에서의 self-invocation은 프록시를 거치지 않는다 — 즉 process()에만 @Transactional을
    // 붙여두면 이 경로로 호출될 때는 트랜잭션이 전혀 시작되지 않고, 각 repository.save() 호출이
    // 개별적으로 자동 커밋되어 버린다. 실제로 이 버그 때문에 persistResults()의 저장은 성공하지만
    // markCompleted()가 detached 상태의 analysis 엔티티에만 반영되고 DB에는 끝내 반영되지 않아
    // 분석 결과는 이미 나왔는데도 status가 영원히 PENDING/PROCESSING에 멈춰 있는 문제가 있었다
    // (같은 클래스의 @Async self-invocation 문제와 동일한 원인, 다른 애노테이션에서 재발한 케이스).
    // REQUIRES_NEW가 필요한 이유는 별개다: Spring은 @TransactionalEventListener(AFTER_COMMIT) 메서드에
    // 기본 전파(REQUIRED)의 @Transactional을 붙이는 것 자체를 기동 시점에 막는다 — AFTER_COMMIT은
    // 원본 트랜잭션이 이미 끝난 뒤 실행되므로 "참여할" 트랜잭션이 없기 때문에, REQUIRES_NEW(새 트랜잭션
    // 시작) 또는 NOT_SUPPORTED(비트랜잭션)로 의도를 명시하도록 강제한다.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAnalysisRequested(AnalysisRequestedEvent event) {
        process(event.analysisId(), event.request());
    }

    @Transactional
    public void process(Long analysisId, AnalysisCreateRequest request) {
        Analysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis == null) {
            log.warn("비동기 분석 처리 중 analysis를 찾을 수 없음 (analysisId={})", analysisId);
            return;
        }

        analysis.markProcessing();

        try {
            AiRecommendResponseDto aiResponse = aiServerClient.recommend(new AiRecommendRequestDto(
                analysisId,
                buildUserProfilePayload(request),
                SUPPORTED_COUNTRIES
            ));
            persistResults(analysis, aiResponse);
            analysis.markCompleted(aiResponse.modelVersion(), aiResponse.dataVersion());
        } catch (RuntimeException exception) {
            log.error("AI 분석 요청 실패 (analysisId={})", analysisId, exception);
            // GlobalExceptionHandler(@RestControllerAdvice)는 이 스레드(비동기 처리 후)의 예외를
            // 잡지 못하므로, 원래 동기 흐름에서 하던 Sentry/Slack 보고를 여기서 직접 해줘야 한다.
            Sentry.captureException(exception);
            slackNotifier.notifyError("AI 분석 처리 실패", "/api/analyses/" + analysisId, "-", exception.getMessage());
            analysis.markFailed(exception.getMessage());
        }
    }

    private void persistResults(Analysis analysis, AiRecommendResponseDto aiResponse) {
        for (AiRecommendResponseDto.CountryResult result : aiResponse.results()) {
            VisaProgram visaProgram = visaProgramRepository.findFirstByCountryCodeAndIsActiveTrue(result.countryCode())
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 국가입니다: " + result.countryCode()));

            AnalysisCountryResult savedResult = resultRepository.save(new AnalysisCountryResult(
                analysis.getAnalysisId(),
                visaProgram.getVisaProgramId(),
                (short) result.rank(),
                result.totalScore(),
                result.ruleScore(),
                result.environmentScore(),
                result.careerSimilarity(),
                result.preferenceScore(),
                RuleStatusMapper.toEligibilityStatus(result.ruleStatus())
            ));

            short order = 1;
            for (String strength : result.strengths()) {
                reasonRepository.save(new AnalysisResultReason(savedResult.getResultId(), "STRENGTH", strength, order++));
            }
            order = 1;
            for (String improvement : result.improvements()) {
                reasonRepository.save(new AnalysisResultReason(savedResult.getResultId(), "IMPROVEMENT", improvement, order++));
            }
        }
    }

    private Map<String, Object> buildUserProfilePayload(AnalysisCreateRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("age", request.age());
        payload.put("education", request.education());
        payload.put("major", request.major());
        payload.put("occupation", request.occupation());
        payload.put("experienceYears", request.experienceYears());
        payload.put("languageTest", request.languageTest());
        payload.put("languageScore", request.languageScore());
        payload.put("careerText", request.careerText());
        payload.put("fundsRange", request.fundsRange());
        payload.put("familyAccompanied", request.familyAccompanied());
        payload.put("preferredCountry", request.preferredCountry());
        return payload;
    }
}
