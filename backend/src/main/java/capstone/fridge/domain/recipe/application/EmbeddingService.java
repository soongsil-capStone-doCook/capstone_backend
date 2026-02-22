package capstone.fridge.domain.recipe.application;

import capstone.fridge.domain.recipe.dto.HybridEmbeddingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final RestTemplate restTemplate; // Bean으로 주입받는 것을 권장합니다.

    @Value("${app.embedding.url}")
    private String embeddingServerUrl;

    public HybridEmbeddingResponse getHybridEmbedding(String text) {
        Map<String, String> request = Map.of("text", text);

        // Map 대신 우리가 만든 DTO로 바로 받습니다. 훨씬 안전합니다!
        HybridEmbeddingResponse response = restTemplate.postForObject(embeddingServerUrl, request, HybridEmbeddingResponse.class);

        if (response == null || response.getDense() == null || response.getSparse() == null) {
            throw new RuntimeException("임베딩 서버로부터 하이브리드 데이터를 받지 못했습니다.");
        }

        return response;
    }

    public List<Float> getEmbedding(String text) {
        Map<String, String> request = Map.of("text", text);

        // 파이썬 서버의 응답 키인 "embedding"에 맞춰 수정
        Map<String, Object> response = restTemplate.postForObject(embeddingServerUrl, request, Map.class);

        if (response == null || !response.containsKey("embedding")) {
            throw new RuntimeException("임베딩 서버로부터 응답을 받지 못했습니다.");
        }

        List<Number> rawVector = (List<Number>) response.get("embedding");

        return rawVector.stream()
                .map(Number::floatValue)
                .collect(Collectors.toList());
    }
}