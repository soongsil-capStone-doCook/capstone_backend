package codeshot.photogram.domain.login.application;

import codeshot.photogram.domain.login.domain.LoginType;
import codeshot.photogram.domain.login.domain.SocialProvider;
import codeshot.photogram.domain.login.domain.entity.SocialLogin;
import codeshot.photogram.domain.login.domain.repository.SocialLoginRepository;
import codeshot.photogram.domain.member.domain.Visibility;
import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.member.domain.repository.MemberRepository;
import codeshot.photogram.global.oauth2.handler.domain.wrapper.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;
    private final SocialLoginRepository socialLoginRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        // OAuth 제공자 정보
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());

        // 유저 정보
        String providerId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String profileImageUrl = oauth2User.getAttribute("picture");

        // 소셜 로그인 계정 찾기
        SocialLogin socialLogin = socialLoginRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // 없으면 새로 만들기
                    Member newMember = Member.builder()
                            .name(name)
                            .nickName(generateRandomNickName()) // 친구 검색 등에서 닉네임 반드시 필요
                            .profileImageUrl(profileImageUrl)
                            .loginType(LoginType.SOCIAL)
                            .visibility(Visibility.PUBLIC)
                            .build();

                    memberRepository.save(newMember);

                    SocialLogin newSocialLogin = SocialLogin.builder()
                            .email(email)
                            .provider(provider)
                            .providerId(providerId)
                            .member(newMember)
                            .build();

                    return socialLoginRepository.save(newSocialLogin);
                });

        Member member = socialLogin.getMember();

        return new CustomOAuth2User(
                member.getAuthorities(),
                oauth2User.getAttributes(),
                "sub", // or "id" depending on provider
                member
        );

    }

    private String generateRandomNickName() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }
}

