package capstone.fridge.global.client;

import capstone.fridge.global.client.dto.FastApiOcrDtos;
import capstone.fridge.global.client.dto.FastApiPlaceDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Component
public class FastApiClient {

    private final RestClient restClient;

    public FastApiClient(@Value("${app.fastapi.base-url}") String baseUrl) {

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();

        // FastAPI로 나가는 요청을 콘솔에 찍는 인터셉터
        ClientHttpRequestInterceptor logInterceptor = (request, body, execution) -> {
            System.out.println("[FASTAPI OUT] " + request.getMethod() + " " + request.getURI());
            System.out.println("[FASTAPI OUT] headers=" + request.getHeaders());
            System.out.println("[FASTAPI OUT] body=" + new String(body, StandardCharsets.UTF_8));
            return execution.execute(request, body);
        };

        // RestClient 만들 때 .requestInterceptor(...)로 붙임
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)                // ex) http://localhost:8001
                .requestFactory(rf)              // HTTP/1.1 강제
                .requestInterceptor(logInterceptor)
                .build();
    }

    /**
     *  이미지 -> FastAPI /ocr (multipart/form-data)
     * FastAPI 응답: {"rawText":"...", "items":[...]}
     */
    public FastApiOcrDtos.OcrRes ocr(MultipartFile image) {
        try {
            ByteArrayResource file = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return (image.getOriginalFilename() == null) ? "image.jpg" : image.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            // FastAPI는 key가 "image"여야 함 (main.py: File(...))
            body.add("image", file);

            return restClient.post()
                    .uri("/ocr")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(FastApiOcrDtos.OcrRes.class);

        } catch (Exception e) {
            throw new RuntimeException("FastAPI /ocr 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * items -> FastAPI /place (application/json)
     */
    public FastApiPlaceDtos.PlaceRes place(FastApiPlaceDtos.PlaceReq req) {
        return restClient.post()
                .uri("/place")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(FastApiPlaceDtos.PlaceRes.class);
    }
}
