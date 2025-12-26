package capstone.fridge.domain.auth.api;

import capstone.fridge.domain.auth.api.dto.AuthDtos;
import capstone.fridge.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/kakao")
    public ResponseEntity<AuthDtos.TokenRes> kakaoLogin(@RequestBody AuthDtos.KakaoLoginReq req) {
        return ResponseEntity.ok(authService.loginKakao(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.TokenRes> refresh(@RequestBody AuthDtos.RefreshReq req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody AuthDtos.LogoutReq req) {
        authService.logout(req);
        return ResponseEntity.ok().build();
    }
}
