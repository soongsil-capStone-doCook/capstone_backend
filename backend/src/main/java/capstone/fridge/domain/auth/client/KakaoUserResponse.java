package capstone.fridge.domain.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount,
        Properties properties
) {
    public record KakaoAccount(String email) {}
    public record Properties(String nickname) {}
}
