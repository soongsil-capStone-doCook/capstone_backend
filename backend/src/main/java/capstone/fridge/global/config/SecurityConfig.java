package capstone.fridge.global.config;

import capstone.fridge.global.config.security.JwtAuthenticationFilter;
import capstone.fridge.global.config.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/fridge/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}



//package capstone.fridge.global.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                // CSRF 비활성화 (API 서버이므로)
//                .csrf(AbstractHttpConfigurer::disable)
//                // 기본 폼 로그인 비활성화 (JWT/OAuth 쓸거니까)
//                .formLogin(AbstractHttpConfigurer::disable)
//                // HTTP Basic 인증 비활성화
//                .httpBasic(AbstractHttpConfigurer::disable)
//
//                // 경로별 인가 설정
//                .authorizeHttpRequests(auth -> auth
//                        // Swagger UI 관련 경로 허용
//                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
//                        // 로그인/회원가입 API 허용
//                        .requestMatchers("/api/v1/auth/**", "/api/v1/members/signup").permitAll()
//                        // 그 외 모든 요청은 인증 필요
//                        .anyRequest().authenticated()
//                );
//
//        return http.build();
//    }
//}


