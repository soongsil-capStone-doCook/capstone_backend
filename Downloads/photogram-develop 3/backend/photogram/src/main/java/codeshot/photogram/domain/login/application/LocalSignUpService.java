package codeshot.photogram.domain.login.application;

import codeshot.photogram.domain.login.dto.LocalSignUpRequest;
import org.springframework.web.multipart.MultipartFile;

public interface LocalSignUpService {
    void signUp(LocalSignUpRequest request, MultipartFile profileImage);

    public String uploadFile(MultipartFile file);
}
