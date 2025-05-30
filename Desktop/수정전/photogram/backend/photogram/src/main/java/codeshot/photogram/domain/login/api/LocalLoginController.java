package codeshot.photogram.domain.login.api;

import codeshot.photogram.domain.login.application.LocalLoginService;
import codeshot.photogram.domain.login.dto.LocalLoginRequest;
import codeshot.photogram.domain.login.dto.LocalLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class LocalLoginController {

    private final LocalLoginService localLoginService;

    @PostMapping("/login")
    public ResponseEntity<LocalLoginResponse> login(@RequestBody LocalLoginRequest request) {
        LocalLoginResponse response = localLoginService.login(request);
        return ResponseEntity.ok(response);
    }

}
