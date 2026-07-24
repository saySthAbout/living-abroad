package com.livingabroad.backend.service;

import com.livingabroad.backend.dto.analysis.AnalysisCreateRequest;
import com.livingabroad.backend.dto.analysis.AnalysisCreateResponse;
import com.livingabroad.backend.dto.analysis.AnalysisInputResponse;
import com.livingabroad.backend.entity.Analysis;
import com.livingabroad.backend.event.AnalysisRequestedEvent;
import com.livingabroad.backend.exception.AnalysisNotFoundException;
import com.livingabroad.backend.repository.AnalysisCountryResultRepository;
import com.livingabroad.backend.repository.AnalysisRepository;
import com.livingabroad.backend.repository.AnalysisResultReasonRepository;
import com.livingabroad.backend.repository.VisaProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private AnalysisCountryResultRepository resultRepository;

    @Mock
    private AnalysisResultReasonRepository reasonRepository;

    @Mock
    private VisaProgramRepository visaProgramRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AnalysisService analysisService;

    @Test
    void createAnalysisSavesPendingAnalysisAndPublishesRequestedEventInsteadOfCallingAiInline() throws Exception {
        AnalysisCreateRequest request = new AnalysisCreateRequest(
            30, "BACHELOR", "컴퓨터공학", "소프트웨어 엔지니어", new BigDecimal("5"),
            "IELTS_GENERAL", new BigDecimal("7.0"), "30M_50M", true, "CAN",
            "5년간 백엔드 개발자로 근무하며 대규모 트래픽 서비스를 운영했습니다. ".repeat(5)
        );
        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis analysis = invocation.getArgument(0);
            setField(analysis, "analysisId", 42L);
            return analysis;
        });

        AnalysisCreateResponse response = analysisService.createAnalysis(100L, request);

        assertThat(response.analysisId()).isEqualTo(42L);
        assertThat(response.status()).isEqualTo("PENDING");

        ArgumentCaptor<AnalysisRequestedEvent> eventCaptor = ArgumentCaptor.forClass(AnalysisRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().analysisId()).isEqualTo(42L);
        assertThat(eventCaptor.getValue().request()).isEqualTo(request);
    }

    @Test
    void getLatestInputMapsStoredFieldsBackToRequestShape() throws Exception {
        Analysis analysis = newAnalysis(
            (short) 30, "BACHELOR", "컴퓨터공학", "소프트웨어 엔지니어", new BigDecimal("5"),
            "IELTS_GENERAL", new BigDecimal("7.0"), 40_000_000L, true, "CAN", "5년간 백엔드 개발..."
        );
        when(analysisRepository.findFirstByUserIdOrderByRequestedAtDesc(100L)).thenReturn(Optional.of(analysis));

        AnalysisInputResponse response = analysisService.getLatestInput(100L);

        assertThat(response.age()).isEqualTo(30);
        assertThat(response.education()).isEqualTo("BACHELOR");
        assertThat(response.fundsRange()).isEqualTo("30M_50M");
        assertThat(response.familyAccompanied()).isTrue();
        assertThat(response.preferredCountry()).isEqualTo("CAN");
    }

    @Test
    void getLatestInputMapsNullPreferredCountryBackToAny() throws Exception {
        Analysis analysis = newAnalysis(
            (short) 25, "MASTER", "경영학", "회계사", new BigDecimal("3"),
            null, null, 5_000_000L, false, null, "경력 설명..."
        );
        when(analysisRepository.findFirstByUserIdOrderByRequestedAtDesc(100L)).thenReturn(Optional.of(analysis));

        AnalysisInputResponse response = analysisService.getLatestInput(100L);

        assertThat(response.preferredCountry()).isEqualTo("ANY");
        assertThat(response.fundsRange()).isEqualTo("UNDER_10M");
    }

    @Test
    void getLatestInputThrowsWhenUserHasNoAnalysis() {
        when(analysisRepository.findFirstByUserIdOrderByRequestedAtDesc(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.getLatestInput(404L))
            .isInstanceOf(AnalysisNotFoundException.class);
    }

    private Analysis newAnalysis(
        Short age, String education, String major, String occupation, BigDecimal experienceYears,
        String languageTest, BigDecimal languageScore, Long fundsKrw, boolean familyAccompanied,
        String preferredCountry, String careerText
    ) throws Exception {
        Analysis analysis = new Analysis(
            100L, age, education, major, occupation, experienceYears,
            languageTest, languageScore, fundsKrw, familyAccompanied, preferredCountry, careerText
        );
        setField(analysis, "analysisId", 1L);
        return analysis;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
