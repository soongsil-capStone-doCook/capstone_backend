package capstone.fridge.domain.auth.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KakaoClient {

    private final RestClient restClient = RestClient.create();

    public KakaoUserResponse getUser(String kakaoAccessToken) {
        return restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(KakaoUserResponse.class);
    }
}
