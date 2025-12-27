package capstone.fridge.global.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        // [디버깅 로그 1] 헤더가 들어왔는지 확인
        System.out.println("DEBUG: Authorization Header = " + auth);

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            // [디버깅 로그 2] 토큰 분리 확인
            System.out.println("DEBUG: Extracted Token = " + token);

            if (jwtTokenProvider.validate(token)) {
                Long memberId = jwtTokenProvider.getMemberId(token);

                // [디버깅 로그 3] ID 추출 확인
                System.out.println("DEBUG: Token Validated. Member ID = " + memberId);

                var authentication = new UsernamePasswordAuthenticationToken(
                        memberId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // [디버깅 로그 4] 컨텍스트 설정 완료
                System.out.println("DEBUG: SecurityContext Set Authentication Success");
            } else {
                // [디버깅 로그 5] 토큰 검증 실패 (만료되거나 위조됨)
                System.out.println("DEBUG: Token Validation Failed!");
            }
        } else {
            // [디버깅 로그 6] 헤더가 없거나 Bearer 형식이 아님
            System.out.println("DEBUG: No valid Authorization header found");
        }

        filterChain.doFilter(request, response);
    }
}
