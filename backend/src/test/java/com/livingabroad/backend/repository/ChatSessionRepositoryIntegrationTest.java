package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.ChatSession;
import com.livingabroad.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

// ChatSessionRepositoryTest(순수 Mockito)는 실제 SQL을 전혀 실행하지 않기 때문에, "필터 없음"을
// 표현하려고 null을 바인딩할 때 Hibernate가 파라미터 타입(String)을 값에서 추론하지 못해
// PostgreSQL이 bytea로 오추론하는 문제("function lower(bytea) does not exist")를 잡아내지 못했다.
// 이 테스트는 실제 DB에 대고 그 정확한 호출 경로(빈 문자열 바인딩)를 실행해서 회귀를 방지한다.
@SpringBootTest
class ChatSessionRepositoryIntegrationTest {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void searchWithBlankFiltersDoesNotFailWithByteaTypeInferenceError() {
        User user = userRepository.save(new User(
            "chat-history-search-test-" + System.nanoTime() + "@example.com",
            passwordEncoder.encode("password1234"),
            "Chat History Search Test"
        ));

        chatSessionRepository.save(new ChatSession(user.getUserId(), null, null, "Express Entry 관련 질문", "CAN"));

        Page<ChatSession> result = chatSessionRepository.search(user.getUserId(), "", "", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void searchWithKeywordFiltersByMatchingSessionTitle() {
        User user = userRepository.save(new User(
            "chat-history-search-test-" + System.nanoTime() + "@example.com",
            passwordEncoder.encode("password1234"),
            "Chat History Search Test"
        ));

        chatSessionRepository.save(new ChatSession(user.getUserId(), null, null, "Express Entry 관련 질문", "CAN"));
        chatSessionRepository.save(new ChatSession(user.getUserId(), null, null, "Skilled Worker 비자 문의", "GBR"));

        Page<ChatSession> result = chatSessionRepository.search(user.getUserId(), "", "Express", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSessionTitle()).contains("Express Entry");
    }
}
