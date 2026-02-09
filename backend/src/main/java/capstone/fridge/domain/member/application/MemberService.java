package capstone.fridge.domain.member.application;

import capstone.fridge.domain.member.dto.MemberRequestDTO;
import capstone.fridge.domain.member.dto.MemberResponseDTO;

public interface MemberService {

    MemberResponseDTO.UserInfoDTO getUserInfo(Long memberId);

    MemberResponseDTO.UserPreferencesDTO setUserPreferences(Long memberId, MemberRequestDTO.UserPreferencesDTO request);

    MemberResponseDTO.UserScrapsDTO getUserScraps(Long memberId);

    MemberResponseDTO.OnboardingStatusDTO checkOnboardingStatus(Long memberId);

    String updateFcmToken(Long memberId, String token);
}
