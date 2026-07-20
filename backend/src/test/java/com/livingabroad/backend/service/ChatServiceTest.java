package com.livingabroad.backend.service;

import com.livingabroad.backend.client.AiServerClient;
import com.livingabroad.backend.dto.ai.AiRagResponseDto;
import com.livingabroad.backend.dto.chat.ChatAnswerResponse;
import com.livingabroad.backend.dto.chat.ChatQuestionRequest;
import com.livingabroad.backend.entity.ChatMessage;
import com.livingabroad.backend.entity.ChatSession;
import com.livingabroad.backend.exception.ChatSessionNotFoundException;
import com.livingabroad.backend.repository.ChatMessageRepository;
import com.livingabroad.backend.repository.ChatMessageSourceRepository;
import com.livingabroad.backend.repository.ChatSessionRepository;
import com.livingabroad.backend.repository.PolicyChunkRepository;
import com.livingabroad.backend.repository.PolicyDocumentRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatMessageSourceRepository chatMessageSourceRepository;

    @Mock
    private PolicyChunkRepository policyChunkRepository;

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    @Mock
    private VisaProgramRepository visaProgramRepository;

    @Mock
    private AiServerClient aiServerClient;

    @InjectMocks
    private ChatService chatService;

    @Test
    void askQuestionCreatesNewSessionWhenNoneProvided() throws Exception {
        ChatQuestionRequest request = new ChatQuestionRequest(null, "Express Entry 서류가 뭐야?", "CAN", "CA_EE_FSW", null);

        ChatSession savedSession = newSession(1L, 100L);
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(savedSession);

        ChatMessage userMessage = newMessage(1L, 1L, "USER");
        ChatMessage assistantMessage = newMessage(2L, 1L, "ASSISTANT");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(userMessage, assistantMessage);

        AiRagResponseDto aiResponse = new AiRagResponseDto(
            "여권, 언어시험 결과, 학력 인증서가 필요합니다.",
            true,
            List.of(new AiRagResponseDto.Source(11L, "Express Entry 준비 서류", "https://canada.ca/docs", "2026-07-19", BigDecimal.valueOf(0.85))),
            "rag-1.0.0"
        );
        when(aiServerClient.ragAnswer(any())).thenReturn(aiResponse);

        ChatAnswerResponse response = chatService.askQuestion(100L, request);

        assertThat(response.sessionId()).isEqualTo(1L);
        assertThat(response.answerable()).isTrue();
        assertThat(response.answer()).isEqualTo("여권, 언어시험 결과, 학력 인증서가 필요합니다.");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).title()).isEqualTo("Express Entry 준비 서류");
    }

    @Test
    void askQuestionRejectsSessionNotOwnedByUser() throws Exception {
        ChatQuestionRequest request = new ChatQuestionRequest(5L, "질문", "CAN", "CA_EE_FSW", null);
        ChatSession othersSession = newSession(5L, 999L);
        when(chatSessionRepository.findById(5L)).thenReturn(Optional.of(othersSession));

        assertThatThrownBy(() -> chatService.askQuestion(100L, request))
            .isInstanceOf(ChatSessionNotFoundException.class);
    }

    @Test
    void getSessionHistoryRejectsSessionNotOwnedByUser() throws Exception {
        ChatSession othersSession = newSession(5L, 999L);
        when(chatSessionRepository.findById(5L)).thenReturn(Optional.of(othersSession));

        assertThatThrownBy(() -> chatService.getSessionHistory(100L, 5L))
            .isInstanceOf(ChatSessionNotFoundException.class);
    }

    @Test
    void getSessionHistoryThrowsWhenSessionMissing() {
        when(chatSessionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getSessionHistory(100L, 404L))
            .isInstanceOf(ChatSessionNotFoundException.class);
    }

    private ChatSession newSession(Long sessionId, Long userId) throws Exception {
        ChatSession session = new ChatSession(userId, null, null, "title");
        setField(session, "sessionId", sessionId);
        return session;
    }

    private ChatMessage newMessage(Long messageId, Long sessionId, String role) throws Exception {
        ChatMessage message = new ChatMessage(sessionId, role, "content", role.equals("ASSISTANT") ? true : null);
        setField(message, "messageId", messageId);
        return message;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
