package capstone.fridge.domain.member.application;

import capstone.fridge.domain.member.converter.MemberConverter;
import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.member.domain.entity.MemberPreference;
import capstone.fridge.domain.member.domain.repository.MemberPreferenceRepository;
import capstone.fridge.domain.member.domain.repository.MemberRepository;
import capstone.fridge.domain.member.dto.MemberRequestDTO;
import capstone.fridge.domain.member.dto.MemberResponseDTO;
import capstone.fridge.domain.member.exception.memberException;
import capstone.fridge.domain.scrap.domain.entity.RecipeScrap;
import capstone.fridge.domain.scrap.domain.repository.RecipeScrapRepository;
import capstone.fridge.global.error.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberPreferenceRepository memberPreferenceRepository;
    private final RecipeScrapRepository recipeScrapRepository;

    @Override
    public MemberResponseDTO.UserInfoDTO getUserInfo(String kakaoId) {
        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        return MemberConverter.toUserInfoDTO(member);
    }

    @Override
    @Transactional
    public MemberResponseDTO.UserPreferencesDTO setUserPreferences(String kakaoId, MemberRequestDTO.UserPreferencesDTO request) {
        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        // 1. 나이, 성별 업데이트 (값이 있을 때만 Entity 내부에서 변경됨)
        member.updateProfile(request.getAge(), request.getGender());

        // 2. 기존 선호/기피 정보 모두 삭제 (Full Update 방식)
        List<MemberPreference> existingPreferences = memberPreferenceRepository.findAllByMember(member);
        memberPreferenceRepository.deleteAll(existingPreferences);

        // 3. 새로운 정보 리스트 생성 및 저장
        List<MemberPreference> newPreferences = MemberConverter.toMemberPreferenceEntities(member, request);
        memberPreferenceRepository.saveAll(newPreferences);

        // 4. Converter에 member 객체도 함께 전달 (나이, 성별 응답을 위해)
        return MemberConverter.toUserPreferencesDTO(member, newPreferences);
    }

    // 3. 찜한 레시피 목록 조회
    @Override
    public MemberResponseDTO.UserScrapsDTO getUserScraps(String kakaoId) {
        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        // 해당 멤버가 찜한 레시피 목록 조회
        List<RecipeScrap> scrapList = recipeScrapRepository.findAllByMemberWithRecipe(member);

        return MemberConverter.toUserScrapsDTO(scrapList);
    }

    @Override
    public MemberResponseDTO.OnboardingStatusDTO checkOnboardingStatus(String kakaoId) {
        // 1. 회원 조회
        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        // 2. 온보딩 여부 판단 로직
        boolean isOnboarded = (member.getAge() != null && member.getGender() != null);

        // 3. Converter를 사용하여 결과 반환
        return MemberConverter.toOnboardingStatusDTO(member, isOnboarded);
    }
}