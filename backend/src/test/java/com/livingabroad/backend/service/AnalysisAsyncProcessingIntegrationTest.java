package com.livingabroad.backend.service;

import com.livingabroad.backend.dto.analysis.AnalysisCreateRequest;
import com.livingabroad.backend.dto.analysis.AnalysisCreateResponse;
import com.livingabroad.backend.entity.User;
import com.livingabroad.backend.repository.AnalysisRepository;
import com.livingabroad.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// 순수 Mockito 유닛테스트(AnalysisProcessorTest, AnalysisServiceTest)는 Spring AOP 프록시를 전혀
// 안 거치기 때문에, onAnalysisRequested()가 process(...)를 같은 빈 안에서 직접 호출할 때 생기는
// self-invocation 트랜잭션 버그(@Transactional이 무시되는 문제)를 잡아내지 못한다. 이 테스트는
// 실제 Spring 컨텍스트 + 실제 DB로 이벤트 발행부터 커밋까지 전체 경로를 태워서, 분석 상태가
// PENDING/PROCESSING에 무한정 멈추지 않고 실제로 진행되는지 확인한다.
@SpringBootTest
class AnalysisAsyncProcessingIntegrationTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createAnalysisTransitionsOutOfPendingWithoutHanging() throws Exception {
        User user = userRepository.save(new User(
            "async-integration-test-" + System.nanoTime() + "@example.com",
            passwordEncoder.encode("password1234"),
            "Async Integration Test"
        ));

        AnalysisCreateRequest request = new AnalysisCreateRequest(
            30, "BACHELOR", "Computer Science", "Software Engineer", new BigDecimal("5"),
            "IELTS_GENERAL", new BigDecimal("7.0"), "30M_50M", true, "CAN",
            "Worked as a backend engineer for five years building and operating distributed systems."
        );

        AnalysisCreateResponse response = analysisService.createAnalysis(user.getUserId(), request);
        assertThat(response.status()).isEqualTo("PENDING");

        String finalStatus = "PENDING";
        for (int i = 0; i < 40 && ("PENDING".equals(finalStatus) || "PROCESSING".equals(finalStatus)); i++) {
            Thread.sleep(250);
            finalStatus = analysisRepository.findById(response.analysisId()).orElseThrow().getAnalysisStatus();
        }

        // ai-server가 테스트 환경에 떠있지 않으므로 FAILED로 끝나는 게 정상 경로다 — 중요한 건
        // COMPLETED든 FAILED든 상태가 실제로 바뀐다는 것이지, 어떤 값으로 끝나는지가 아니다.
        assertThat(finalStatus).isIn("COMPLETED", "FAILED");
    }
}
