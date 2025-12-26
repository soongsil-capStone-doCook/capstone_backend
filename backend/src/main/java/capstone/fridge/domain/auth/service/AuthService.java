package capstone.fridge.domain.auth.service;

import capstone.fridge.domain.auth.api.dto.AuthDtos;
import capstone.fridge.domain.auth.client.KakaoClient;
import capstone.fridge.domain.auth.domain.RefreshToken;
import capstone.fridge.domain.auth.domain.RefreshTokenRepository;
import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.member.domain.repository.MemberRepository;
import capstone.fridge.global.config.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final KakaoClient kakaoClient;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthDtos.TokenRes loginKakao(AuthDtos.KakaoLoginReq req) {
        var token = kakaoClient.getToken(req.code());
        var me = kakaoClient.getUser(token.accessToken());

        String kakaoId = String.valueOf(me.id());
        String nickname = (me.properties() != null && me.properties().nickname() != null)
                ? me.properties().nickname()
                : "kakao-user";

        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .kakaoId(kakaoId)
                                .nickname(nickname)
                                .email(me.kakaoAccount() == null ? null : me.kakaoAccount().email())
                                .profileImageUrl(null)
                                .build()
                ));

        String accessJwt = jwtTokenProvider.createAccessToken(member.getId());
        String refreshJwt = jwtTokenProvider.createRefreshToken(member.getId());

        LocalDateTime refreshExp = jwtTokenProvider.getExpiration(refreshJwt)
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        RefreshToken rt = refreshTokenRepository.findById(member.getId())
                .orElse(RefreshToken.builder()
                        .memberId(member.getId())
                        .token(refreshJwt)
                        .expiresAt(refreshExp)
                        .build());

        rt.rotate(refreshJwt, refreshExp);
        refreshTokenRepository.save(rt);

        return new AuthDtos.TokenRes(accessJwt, refreshJwt, member.getId(), member.getNickname());
    }

    public AuthDtos.TokenRes refresh(AuthDtos.RefreshReq req) {
        String refreshToken = req.refreshToken();
        if (!jwtTokenProvider.validate(refreshToken)) {
            throw new IllegalArgumentException("invalid refresh token");
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);

        RefreshToken saved = refreshTokenRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("refresh token not found"));

        if (!saved.getToken().equals(refreshToken)) {
            throw new IllegalArgumentException("refresh token mismatch");
        }

        String newAccess = jwtTokenProvider.createAccessToken(memberId);
        String newRefresh = jwtTokenProvider.createRefreshToken(memberId);

        LocalDateTime refreshExp = jwtTokenProvider.getExpiration(newRefresh)
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        saved.rotate(newRefresh, refreshExp);
        refreshTokenRepository.save(saved);

        Member member = memberRepository.findById(memberId).orElseThrow();
        return new AuthDtos.TokenRes(newAccess, newRefresh, memberId, member.getNickname());
    }

    public void logout(AuthDtos.LogoutReq req) {
        String refreshToken = req.refreshToken();
        if (!jwtTokenProvider.validate(refreshToken)) return;

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        refreshTokenRepository.deleteById(memberId); // 가장 단순: refresh 토큰 삭제
    }
}
