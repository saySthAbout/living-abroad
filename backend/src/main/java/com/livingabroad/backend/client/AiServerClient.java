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

@Component
public class AiServerClient {

    private final RestClient restClient;

    public AiServerClient(@Value("${app.ai-server.base-url}") String baseUrl) {
        // FastAPI/uvicorn only speaks HTTP/1.1; the JDK HttpClient defaults to
        // attempting an HTTP/2 cleartext upgrade, which uvicorn rejects at the
        // protocol level (silently dropping the request body). Force HTTP/1.1.
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
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
