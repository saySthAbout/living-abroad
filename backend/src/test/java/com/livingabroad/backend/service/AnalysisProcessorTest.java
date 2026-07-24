package com.livingabroad.backend.service;

import com.livingabroad.backend.client.AiServerClient;
import com.livingabroad.backend.dto.ai.AiRecommendResponseDto;
import com.livingabroad.backend.dto.analysis.AnalysisCreateRequest;
import com.livingabroad.backend.entity.Analysis;
import com.livingabroad.backend.entity.AnalysisCountryResult;
import com.livingabroad.backend.entity.VisaProgram;
import com.livingabroad.backend.repository.AnalysisCountryResultRepository;
import com.livingabroad.backend.repository.AnalysisRepository;
import com.livingabroad.backend.repository.AnalysisResultReasonRepository;
import com.livingabroad.backend.repository.VisaProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisProcessorTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private AnalysisCountryResultRepository resultRepository;

    @Mock
    private AnalysisResultReasonRepository reasonRepository;

    @Mock
    private VisaProgramRepository visaProgramRepository;

    @Mock
    private AiServerClient aiServerClient;

    @Mock
    private SlackNotifier slackNotifier;

    @InjectMocks
    private AnalysisProcessor analysisProcessor;

    @Test
    void processMarksProcessingThenCompletedOnSuccess() throws Exception {
        Analysis analysis = newAnalysis(1L);
        when(analysisRepository.findById(1L)).thenReturn(Optional.of(analysis));

        VisaProgram visaProgram = newVisaProgram(10L, "CAN");
        when(visaProgramRepository.findFirstByCountryCodeAndIsActiveTrue("CAN")).thenReturn(Optional.of(visaProgram));
        when(resultRepository.save(any(AnalysisCountryResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiRecommendResponseDto aiResponse = new AiRecommendResponseDto(
            "model-v1", "data-v1",
            List.of(new AiRecommendResponseDto.CountryResult(
                1, "CAN",
                new BigDecimal("85.0"), new BigDecimal("80.0"), new BigDecimal("90.0"), new BigDecimal("70.0"), new BigDecimal("100.0"),
                "MET", List.of("경력 적합"), List.of("영어 점수 보완 필요")
            ))
        );
        when(aiServerClient.recommend(any())).thenReturn(aiResponse);

        analysisProcessor.process(1L, sampleRequest());

        assertThat(analysis.getAnalysisStatus()).isEqualTo("COMPLETED");
        verify(resultRepository).save(any(AnalysisCountryResult.class));
        verifyNoInteractions(slackNotifier);
    }

    @Test
    void processMarksFailedAndReportsToSlackWhenAiServerThrows() throws Exception {
        Analysis analysis = newAnalysis(2L);
        when(analysisRepository.findById(2L)).thenReturn(Optional.of(analysis));
        when(aiServerClient.recommend(any())).thenThrow(new RuntimeException("AI 서버 연결 실패"));

        analysisProcessor.process(2L, sampleRequest());

        assertThat(analysis.getAnalysisStatus()).isEqualTo("FAILED");
        verify(slackNotifier).notifyError(
            "AI 분석 처리 실패", "/api/analyses/2", "-", "AI 서버 연결 실패"
        );
    }

    @Test
    void processDoesNothingWhenAnalysisNoLongerExists() {
        when(analysisRepository.findById(99L)).thenReturn(Optional.empty());

        analysisProcessor.process(99L, sampleRequest());

        verifyNoInteractions(aiServerClient, slackNotifier);
    }

    private AnalysisCreateRequest sampleRequest() {
        return new AnalysisCreateRequest(
            30, "BACHELOR", "컴퓨터공학", "소프트웨어 엔지니어", new BigDecimal("5"),
            "IELTS_GENERAL", new BigDecimal("7.0"), "30M_50M", true, "CAN",
            "5년간 백엔드 개발자로 근무하며 대규모 트래픽 서비스를 운영했습니다. ".repeat(5)
        );
    }

    private Analysis newAnalysis(Long analysisId) throws Exception {
        Analysis analysis = new Analysis(
            100L, (short) 30, "BACHELOR", "컴퓨터공학", "소프트웨어 엔지니어", new BigDecimal("5"),
            "IELTS_GENERAL", new BigDecimal("7.0"), 40_000_000L, true, "CAN", "5년간 백엔드 개발..."
        );
        setField(analysis, "analysisId", analysisId);
        return analysis;
    }

    private VisaProgram newVisaProgram(Long visaProgramId, String countryCode) throws Exception {
        var constructor = VisaProgram.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        VisaProgram visaProgram = constructor.newInstance();
        setField(visaProgram, "visaProgramId", visaProgramId);
        setField(visaProgram, "countryCode", countryCode);
        setField(visaProgram, "programCode", "EXPRESS_ENTRY");
        setField(visaProgram, "programNameKo", "익스프레스 엔트리");
        setField(visaProgram, "programNameEn", "Express Entry");
        return visaProgram;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
