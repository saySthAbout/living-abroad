package com.livingabroad.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

// Slack Incoming Webhook URL은 그 자체로 비밀값(누구든 알면 해당 채널에 스팸을 보낼 수 있음)이라
// 프론트엔드에는 절대 노출하지 않는다 — 서버 사이드(backend/ai-server)에서만 호출한다.
@Service
public class SlackNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackNotifier.class);

    private final RestClient restClient = RestClient.create();

    @Value("${app.slack.webhook-url}")
    private String webhookUrl;

    @Value("${app.environment}")
    private String environment;

    // 알림 발송이 실제 에러 응답을 지연시키지 않도록 비동기로 처리한다.
    @Async
    public void notifyError(String title, String path, String traceId, String detail) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            String text = String.format(
                ":rotating_light: *%s*\n환경: %s\n경로: %s\ntraceId: %s\n%s",
                title, environment, path, traceId, detail
            );
            restClient.post()
                .uri(webhookUrl)
                .body(Map.of("text", text))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Slack 알림 발송 실패", e);
        }
    }
}
