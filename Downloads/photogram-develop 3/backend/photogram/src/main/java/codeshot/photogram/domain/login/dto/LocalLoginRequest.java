package codeshot.photogram.domain.login.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalLoginRequest {
    private String photogramId;
    private String password;
}
