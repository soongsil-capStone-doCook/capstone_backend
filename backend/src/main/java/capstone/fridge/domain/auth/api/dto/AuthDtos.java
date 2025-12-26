package capstone.fridge.domain.auth.api.dto;

public class AuthDtos {

    public record KakaoLoginReq(String code) {}

    public record TokenRes(
            String accessToken,
            String refreshToken,
            Long memberId,
            String nickname
    ) {}

    public record RefreshReq(String refreshToken) {}

    public record LogoutReq(String refreshToken) {}
}
