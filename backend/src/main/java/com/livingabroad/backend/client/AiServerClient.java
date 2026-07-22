package com.livingabroad.backend.client;

import com.livingabroad.backend.dto.ai.AiRagRequestDto;
import com.livingabroad.backend.dto.ai.AiRagResponseDto;
import com.livingabroad.backend.dto.ai.AiRecommendRequestDto;
import com.livingabroad.backend.dto.ai.AiRecommendResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class AiServerClient {

    private final RestClient restClient;

    public AiServerClient(
        @Value("${app.ai-server.base-url}") String baseUrl,
        @Value("${app.ai-server.connect-timeout-ms}") long connectTimeoutMs,
        @Value("${app.ai-server.read-timeout-ms}") long readTimeoutMs
    ) {
        // FastAPI/uvicorn only speaks HTTP/1.1; the JDK HttpClient defaults to
        // attempting an HTTP/2 cleartext upgrade, which uvicorn rejects at the
        // protocol level (silently dropping the request body). Force HTTP/1.1.
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofMillis(connectTimeoutMs))
            .build();

        // 명시적 타임아웃 없이는 ai-server가 응답 없이 멈춰 있을 때(다운이 아니라 hang) OS 기본값까지
        // 무한정 기다리게 되어, nginx의 proxy_read_timeout(기본 60초)이 먼저 끊기면서 사용자는
        // 우리가 만든 503 JSON 대신 nginx의 날것 그대로인 504 페이지를 보게 된다. read-timeout을
        // nginx의 타임아웃보다 짧게 잡아 항상 우리 쪽 GlobalExceptionHandler가 먼저 응답하게 한다.
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
    }

    public AiRecommendResponseDto recommend(AiRecommendRequestDto request) {
        return restClient.post()
            .uri("/ai/recommend")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(AiRecommendResponseDto.class);
    }

    public AiRagResponseDto ragAnswer(AiRagRequestDto request) {
        return restClient.post()
            .uri("/ai/rag/answer")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(AiRagResponseDto.class);
    }
}
