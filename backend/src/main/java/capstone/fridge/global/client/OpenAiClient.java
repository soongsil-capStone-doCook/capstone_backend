package capstone.fridge.global.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.model:gpt-4o-mini}")
    private String model;

    public OpenAiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * OpenAI Chat Completions 호출 후 assistant content 반환.
     */
    public String chat(List<Map<String, String>> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.openai.api-key is not set");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.2
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(CHAT_URL, entity, Map.class);

        if (response.getBody() == null) return null;

        Map choices = (Map) ((List) response.getBody().get("choices")).get(0);
        Map message = (Map) choices.get("message");
        return (String) message.get("content");
    }
}
