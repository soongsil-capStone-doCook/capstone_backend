package codeshot.photogram.global.oauth2.handler;

import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.global.oauth2.handler.domain.wrapper.CustomOAuth2User;
import codeshot.photogram.global.security.jwt.JwtTokenProvider;
import codeshot.photogram.global.security.jwt.dto.JwtToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
        Member member = customUser.getMember();


        JwtToken token = jwtTokenProvider.generateToken(authentication, member);

        // 프론트엔드 서버로 리디렉션
        String redirectUri = "http://localhost:3000/oauth-success?token=" + token.getAccessToken();
        response.sendRedirect(redirectUri);
    }
}

