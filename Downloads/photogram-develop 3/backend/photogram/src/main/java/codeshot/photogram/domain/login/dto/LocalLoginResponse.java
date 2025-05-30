package codeshot.photogram.domain.login.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LocalLoginResponse {
    private String accessToken;
    private String refreshToken;
    private String grantType;
}

