package capstone.fridge.domain.recipe.application;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    // 파이썬 임베딩 서버 URL
    @Value("${app.embedding.url}")
    private String embeddingServerUrl;

    public List<Float> getEmbedding(String text) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, String> request = Map.of("text", text);

        // 1. 응답을 Map으로 받음
        Map response = restTemplate.postForObject(embeddingServerUrl, request, Map.class);

        List<Number> rawVector = (List<Number>) response.get("vector");

        return rawVector.stream()
                .map(Number::floatValue)
                .collect(Collectors.toList());
    }
}