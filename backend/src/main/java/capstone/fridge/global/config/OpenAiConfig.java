package capstone.fridge.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI API 호출 설정.
 * Planner / Generator는 OpenAiClient 서비스를 사용.
 */
@Configuration
public class OpenAiConfig {

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.model:gpt-4o-mini}")
    private String model;

    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
}
