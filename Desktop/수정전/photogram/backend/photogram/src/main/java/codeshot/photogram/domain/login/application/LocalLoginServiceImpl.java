package codeshot.photogram.domain.login.application;

import codeshot.photogram.domain.login.domain.entity.LocalLogin;
import codeshot.photogram.domain.login.domain.repository.LocalLoginRepository;
import codeshot.photogram.domain.login.dto.LocalLoginRequest;
import codeshot.photogram.domain.login.dto.LocalLoginResponse;
import codeshot.photogram.global.security.jwt.JwtTokenProvider;
import codeshot.photogram.global.security.jwt.dto.JwtToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LocalLoginServiceImpl implements LocalLoginService {

    private final LocalLoginRepository localLoginRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LocalLoginResponse login(LocalLoginRequest request) {
        LocalLogin login = localLoginRepository.findByPhotogramId(request.getPhotogramId())
                .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), login.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                login.getMember(), null, login.getMember().getAuthorities()
        );
        JwtToken token = jwtTokenProvider.generateToken(authentication, login.getMember());

        return new LocalLoginResponse(token.getAccessToken(), token.getRefreshToken(), token.getGrantType());
    }
}
