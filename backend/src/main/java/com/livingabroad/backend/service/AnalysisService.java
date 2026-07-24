package com.livingabroad.backend.service;

import com.livingabroad.backend.dto.analysis.AnalysisCreateRequest;
import com.livingabroad.backend.dto.analysis.AnalysisCreateResponse;
import com.livingabroad.backend.dto.analysis.AnalysisDetailResponse;
import com.livingabroad.backend.dto.analysis.AnalysisHistoryPageResponse;
import com.livingabroad.backend.dto.analysis.AnalysisInputResponse;
import com.livingabroad.backend.entity.Analysis;
import com.livingabroad.backend.entity.AnalysisCountryResult;
import com.livingabroad.backend.entity.AnalysisResultReason;
import com.livingabroad.backend.entity.VisaProgram;
import com.livingabroad.backend.event.AnalysisRequestedEvent;
import com.livingabroad.backend.exception.AnalysisAccessDeniedException;
import com.livingabroad.backend.exception.AnalysisNotFoundException;
import com.livingabroad.backend.repository.AnalysisCountryResultRepository;
import com.livingabroad.backend.repository.AnalysisRepository;
import com.livingabroad.backend.repository.AnalysisResultReasonRepository;
import com.livingabroad.backend.repository.VisaProgramRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private static final Map<String, String> COUNTRY_NAMES = Map.of(
        "CAN", "캐나다",
        "AUS", "호주",
        "GBR", "영국"
    );
    private static final String DISCLAIMER = "본 점수는 실제 비자 승인 확률이 아니라 서비스 내부 적합도 점수입니다.";

    private final AnalysisRepository analysisRepository;
    private final AnalysisCountryResultRepository resultRepository;
    private final AnalysisResultReasonRepository reasonRepository;
    private final VisaProgramRepository visaProgramRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AnalysisService(
        AnalysisRepository analysisRepository,
        AnalysisCountryResultRepository resultRepository,
        AnalysisResultReasonRepository reasonRepository,
        VisaProgramRepository visaProgramRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.analysisRepository = analysisRepository;
        this.resultRepository = resultRepository;
        this.reasonRepository = reasonRepository;
        this.visaProgramRepository = visaProgramRepository;
        this.eventPublisher = eventPublisher;
    }

    // AI 호출(느릴 수 있음)은 여기서 하지 않는다. PENDING 상태로 저장만 하고 즉시 응답한 뒤,
    // 트랜잭션이 커밋되면 AnalysisProcessor가 이벤트를 받아 백그라운드로 처리한다(AnalysisProcessor 참고).
    @Transactional
    public AnalysisCreateResponse createAnalysis(Long userId, AnalysisCreateRequest request) {
        String preferredCountryCode = "ANY".equals(request.preferredCountry()) ? null : request.preferredCountry();

        Analysis analysis = new Analysis(
            userId,
            request.age().shortValue(),
            request.education(),
            request.major(),
            request.occupation(),
            request.experienceYears(),
            request.languageTest(),
            request.languageScore(),
            fundsRangeMidpoint(request.fundsRange()),
            Boolean.TRUE.equals(request.familyAccompanied()),
            preferredCountryCode,
            request.careerText()
        );
        Analysis saved = analysisRepository.save(analysis);

        eventPublisher.publishEvent(new AnalysisRequestedEvent(saved.getAnalysisId(), request));

        return new AnalysisCreateResponse(saved.getAnalysisId(), saved.getAnalysisStatus(), saved.getRequestedAt());
    }

    @Transactional(readOnly = true)
    public AnalysisDetailResponse getAnalysisDetail(Long userId, Long analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
            .orElseThrow(AnalysisNotFoundException::new);

        if (!analysis.getUserId().equals(userId)) {
            throw new AnalysisAccessDeniedException();
        }

        List<AnalysisCountryResult> results = resultRepository.findByAnalysisIdOrderByRankPositionAsc(analysisId);
        List<Long> resultIds = results.stream().map(AnalysisCountryResult::getResultId).toList();
        Map<Long, List<AnalysisResultReason>> reasonsByResult = reasonRepository.findByResultIdInOrderBySortOrderAsc(resultIds)
            .stream()
            .collect(Collectors.groupingBy(AnalysisResultReason::getResultId, LinkedHashMap::new, Collectors.toList()));

        List<AnalysisDetailResponse.CountryResultDto> resultDtos = results.stream()
            .map(result -> toResultDto(result, reasonsByResult.getOrDefault(result.getResultId(), List.of())))
            .toList();

        return new AnalysisDetailResponse(
            analysis.getAnalysisId(),
            analysis.getAnalysisStatus(),
            analysis.getCompletedAt(),
            resultDtos,
            DISCLAIMER
        );
    }

    @Transactional(readOnly = true)
    public AnalysisInputResponse getLatestInput(Long userId) {
        Analysis analysis = analysisRepository.findFirstByUserIdOrderByRequestedAtDesc(userId)
            .orElseThrow(AnalysisNotFoundException::new);

        return new AnalysisInputResponse(
            analysis.getInputAge() != null ? analysis.getInputAge().intValue() : null,
            analysis.getInputEducationLevel(),
            analysis.getInputMajor(),
            analysis.getInputCurrentOccupation(),
            analysis.getInputExperienceYears(),
            analysis.getInputLanguageTestType(),
            analysis.getInputLanguageScore(),
            fundsRangeFromMidpoint(analysis.getInputAvailableFundsKrw()),
            analysis.isInputFamilyAccompanied(),
            analysis.getInputPreferredCountryCode() != null ? analysis.getInputPreferredCountryCode() : "ANY",
            analysis.getInputCareerDescription()
        );
    }

    @Transactional(readOnly = true)
    public AnalysisHistoryPageResponse listAnalyses(Long userId, int page, int size) {
        Page<Analysis> analysisPage = analysisRepository.findByUserIdOrderByRequestedAtDesc(
            userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"))
        );

        List<AnalysisHistoryPageResponse.HistoryItem> items = analysisPage.getContent().stream()
            .map(this::toHistoryItem)
            .toList();

        return new AnalysisHistoryPageResponse(page, size, analysisPage.getTotalElements(), items);
    }

    private AnalysisHistoryPageResponse.HistoryItem toHistoryItem(Analysis analysis) {
        return resultRepository.findFirstByAnalysisIdAndRankPosition(analysis.getAnalysisId(), (short) 1)
            .map(top -> {
                VisaProgram visaProgram = visaProgramRepository.findById(top.getVisaProgramId()).orElse(null);
                return new AnalysisHistoryPageResponse.HistoryItem(
                    analysis.getAnalysisId(),
                    visaProgram != null ? visaProgram.getCountryCode() : null,
                    visaProgram != null ? visaProgram.getProgramNameEn() : null,
                    top.getTotalScore(),
                    analysis.getCompletedAt()
                );
            })
            .orElseGet(() -> new AnalysisHistoryPageResponse.HistoryItem(
                analysis.getAnalysisId(), null, null, null, analysis.getCompletedAt()
            ));
    }

    private AnalysisDetailResponse.CountryResultDto toResultDto(AnalysisCountryResult result, List<AnalysisResultReason> reasons) {
        VisaProgram visaProgram = visaProgramRepository.findById(result.getVisaProgramId()).orElse(null);
        List<String> strengths = reasons.stream().filter(r -> "STRENGTH".equals(r.getReasonType())).map(AnalysisResultReason::getReasonContent).toList();
        List<String> improvements = reasons.stream().filter(r -> "IMPROVEMENT".equals(r.getReasonType())).map(AnalysisResultReason::getReasonContent).toList();

        String countryCode = visaProgram != null ? visaProgram.getCountryCode() : null;
        return new AnalysisDetailResponse.CountryResultDto(
            result.getRankPosition(),
            countryCode,
            countryCode != null ? COUNTRY_NAMES.get(countryCode) : null,
            visaProgram != null ? visaProgram.getProgramCode() : null,
            visaProgram != null ? visaProgram.getProgramNameKo() : null,
            result.getTotalScore(),
            result.getRuleScore(),
            result.getEnvironmentScore(),
            result.getCareerSimilarityScore(),
            result.getPreferenceScore(),
            RuleStatusMapper.toRuleStatus(result.getEligibilityStatus()),
            strengths,
            improvements
        );
    }

    private Long fundsRangeMidpoint(String fundsRange) {
        return switch (fundsRange) {
            case "UNDER_10M" -> 5_000_000L;
            case "10M_30M" -> 20_000_000L;
            case "30M_50M" -> 40_000_000L;
            case "OVER_50M" -> 60_000_000L;
            default -> 0L;
        };
    }

    // fundsRangeMidpoint()의 역변환. 저장 시 구간 → 중간값으로 변환하기 때문에
    // 원래 선택했던 구간 문자열은 그 중간값으로부터만 복원할 수 있다.
    private String fundsRangeFromMidpoint(Long midpoint) {
        if (midpoint == null) {
            return null;
        }
        if (midpoint <= 5_000_000L) {
            return "UNDER_10M";
        } else if (midpoint <= 20_000_000L) {
            return "10M_30M";
        } else if (midpoint <= 40_000_000L) {
            return "30M_50M";
        }
        return "OVER_50M";
    }
}
