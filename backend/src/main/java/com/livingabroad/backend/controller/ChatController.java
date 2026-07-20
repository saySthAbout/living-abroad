package com.livingabroad.backend.controller;

import com.livingabroad.backend.dto.chat.ChatAnswerResponse;
import com.livingabroad.backend.dto.chat.ChatQuestionRequest;
import com.livingabroad.backend.dto.chat.ChatSessionResponse;
import com.livingabroad.backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatAnswerResponse> ask(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ChatQuestionRequest request
    ) {
        return ResponseEntity.ok(chatService.askQuestion(Long.valueOf(jwt.getSubject()), request));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponse> getSession(@AuthenticationPrincipal Jwt jwt, @PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.getSessionHistory(Long.valueOf(jwt.getSubject()), sessionId));
    }
}
