package com.livingabroad.backend.service;

import com.livingabroad.backend.client.AiServerClient;
import com.livingabroad.backend.dto.ai.AiRagRequestDto;
import com.livingabroad.backend.dto.ai.AiRagResponseDto;
import com.livingabroad.backend.dto.chat.ChatAnswerResponse;
import com.livingabroad.backend.dto.chat.ChatQuestionRequest;
import com.livingabroad.backend.dto.chat.ChatSessionHistoryPageResponse;
import com.livingabroad.backend.dto.chat.ChatSessionResponse;
import com.livingabroad.backend.entity.ChatMessage;
import com.livingabroad.backend.entity.ChatMessageSource;
import com.livingabroad.backend.entity.ChatSession;
import com.livingabroad.backend.entity.PolicyChunk;
import com.livingabroad.backend.entity.PolicyDocument;
import com.livingabroad.backend.entity.VisaProgram;
import com.livingabroad.backend.exception.ChatSessionNotFoundException;
import com.livingabroad.backend.repository.ChatMessageRepository;
import com.livingabroad.backend.repository.ChatMessageSourceRepository;
import com.livingabroad.backend.repository.ChatSessionRepository;
import com.livingabroad.backend.repository.PolicyChunkRepository;
import com.livingabroad.backend.repository.PolicyDocumentRepository;
import com.livingabroad.backend.repository.VisaProgramRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String DISCLAIMER = "본 답변은 법률 자문이 아닙니다.";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageSourceRepository chatMessageSourceRepository;
    private final PolicyChunkRepository policyChunkRepository;
    private final PolicyDocumentRepository policyDocumentRepository;
    private final VisaProgramRepository visaProgramRepository;
    private final AiServerClient aiServerClient;

    public ChatService(
        ChatSessionRepository chatSessionRepository,
        ChatMessageRepository chatMessageRepository,
        ChatMessageSourceRepository chatMessageSourceRepository,
        PolicyChunkRepository policyChunkRepository,
        PolicyDocumentRepository policyDocumentRepository,
        VisaProgramRepository visaProgramRepository,
        AiServerClient aiServerClient
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageSourceRepository = chatMessageSourceRepository;
        this.policyChunkRepository = policyChunkRepository;
        this.policyDocumentRepository = policyDocumentRepository;
        this.visaProgramRepository = visaProgramRepository;
        this.aiServerClient = aiServerClient;
    }

    @Transactional
    public ChatAnswerResponse askQuestion(Long userId, ChatQuestionRequest request) {
        ChatSession session = request.sessionId() != null
            ? findOwnedSession(userId, request.sessionId())
            : chatSessionRepository.save(new ChatSession(userId, request.analysisId(), null, truncateTitle(request.question()), request.countryCode()));

        chatMessageRepository.save(new ChatMessage(session.getSessionId(), "USER", request.question(), null));

        String visaCode = resolveVisaCode(request.countryCode(), request.visaCode());
        AiRagResponseDto aiResponse = aiServerClient.ragAnswer(
            new AiRagRequestDto(request.question(), request.countryCode(), visaCode, 5)
        );

        ChatMessage assistantMessage = chatMessageRepository.save(new ChatMessage(
            session.getSessionId(), "ASSISTANT", aiResponse.answer(), aiResponse.answerable()
        ));

        List<ChatAnswerResponse.SourceDto> sourceDtos = new ArrayList<>();
        for (AiRagResponseDto.Source source : aiResponse.sources()) {
            chatMessageSourceRepository.save(
                new ChatMessageSource(assistantMessage.getMessageId(), source.chunkId(), source.score())
            );
            sourceDtos.add(new ChatAnswerResponse.SourceDto(source.title(), source.url(), source.verifiedAt()));
        }

        return new ChatAnswerResponse(
            session.getSessionId(),
            assistantMessage.getMessageId(),
            aiResponse.answer(),
            aiResponse.answerable(),
            sourceDtos,
            DISCLAIMER
        );
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getSessionHistory(Long userId, Long sessionId) {
        findOwnedSession(userId, sessionId);

        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<Long> messageIds = messages.stream().map(ChatMessage::getMessageId).toList();

        List<ChatMessageSource> allSources = chatMessageSourceRepository.findByMessageIdIn(messageIds);
        Map<Long, List<ChatMessageSource>> sourcesByMessage = allSources.stream()
            .collect(Collectors.groupingBy(ChatMessageSource::getMessageId));

        List<Long> chunkIds = allSources.stream().map(ChatMessageSource::getChunkId).distinct().toList();
        Map<Long, Long> documentIdByChunk = policyChunkRepository.findByChunkIdIn(chunkIds).stream()
            .collect(Collectors.toMap(PolicyChunk::getChunkId, PolicyChunk::getDocumentId));

        List<Long> documentIds = documentIdByChunk.values().stream().distinct().toList();
        Map<Long, PolicyDocument> documentById = policyDocumentRepository.findByDocumentIdIn(documentIds).stream()
            .collect(Collectors.toMap(PolicyDocument::getDocumentId, doc -> doc));

        List<ChatSessionResponse.MessageDto> messageDtos = messages.stream()
            .map(message -> toMessageDto(message, sourcesByMessage, documentIdByChunk, documentById))
            .toList();

        return new ChatSessionResponse(sessionId, messageDtos);
    }

    private ChatSessionResponse.MessageDto toMessageDto(
        ChatMessage message,
        Map<Long, List<ChatMessageSource>> sourcesByMessage,
        Map<Long, Long> documentIdByChunk,
        Map<Long, PolicyDocument> documentById
    ) {
        List<ChatAnswerResponse.SourceDto> sources = sourcesByMessage
            .getOrDefault(message.getMessageId(), List.of())
            .stream()
            .map(source -> {
                Long documentId = documentIdByChunk.get(source.getChunkId());
                PolicyDocument document = documentId != null ? documentById.get(documentId) : null;
                return document == null ? null : new ChatAnswerResponse.SourceDto(
                    document.getDocumentTitle(),
                    document.getSourceUrl(),
                    document.getVerifiedAt().toLocalDate().toString()
                );
            })
            .filter(Objects::nonNull)
            .toList();

        return new ChatSessionResponse.MessageDto(
            message.getMessageId(),
            message.getMessageRole(),
            message.getMessageContent(),
            message.getAnswerable(),
            sources,
            message.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ChatSessionHistoryPageResponse listSessions(Long userId, String countryCode, String keyword, int page, int size) {
        Page<ChatSession> result = chatSessionRepository.search(
            userId, blankToEmpty(countryCode), blankToEmpty(keyword), PageRequest.of(page, size)
        );

        List<ChatSessionHistoryPageResponse.SessionSummary> items = result.getContent().stream()
            .map(session -> new ChatSessionHistoryPageResponse.SessionSummary(
                session.getSessionId(),
                session.getSessionTitle(),
                session.getCountryCode(),
                session.getCreatedAt(),
                session.getUpdatedAt()
            ))
            .toList();

        return new ChatSessionHistoryPageResponse(page, size, result.getTotalElements(), items);
    }

    private String blankToEmpty(String value) {
        return (value == null || value.isBlank()) ? "" : value;
    }

    private ChatSession findOwnedSession(Long userId, Long sessionId) {
        return chatSessionRepository.findById(sessionId)
            .filter(session -> session.getUserId().equals(userId))
            .orElseThrow(ChatSessionNotFoundException::new);
    }

    private String truncateTitle(String question) {
        return question.length() > 50 ? question.substring(0, 50) : question;
    }

    private String resolveVisaCode(String countryCode, String requestedVisaCode) {
        if (requestedVisaCode != null && !requestedVisaCode.isBlank()) {
            return requestedVisaCode;
        }
        return visaProgramRepository.findFirstByCountryCodeAndIsActiveTrue(countryCode)
            .map(VisaProgram::getProgramCode)
            .orElse(null);
    }
}
