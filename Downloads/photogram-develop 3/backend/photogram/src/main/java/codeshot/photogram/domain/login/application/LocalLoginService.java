package codeshot.photogram.domain.login.application;

import codeshot.photogram.domain.login.dto.LocalLoginRequest;
import codeshot.photogram.domain.login.dto.LocalLoginResponse;

public interface LocalLoginService {
    LocalLoginResponse login(LocalLoginRequest request);
}
