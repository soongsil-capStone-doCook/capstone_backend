package codeshot.photogram.domain.login.api;

import codeshot.photogram.domain.login.application.LocalSignUpService;
import codeshot.photogram.domain.login.dto.LocalSignUpRequest;
import codeshot.photogram.domain.login.dto.LocalSignUpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class LocalSignUpController {

    private final LocalSignUpService localSignUpService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestPart("signUpRequest") LocalSignUpRequest request,
                                    @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        localSignUpService.signUp(request, profileImage);
        return ResponseEntity.ok(new LocalSignUpResponse("회원가입이 성공적으로 완료되었습니다."));
    }
}
