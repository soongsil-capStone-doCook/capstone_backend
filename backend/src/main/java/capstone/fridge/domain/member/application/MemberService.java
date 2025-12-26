package capstone.fridge.domain.member.application;

import capstone.fridge.domain.member.dto.MemberRequestDTO;
import capstone.fridge.domain.member.dto.MemberResponseDTO;

public interface MemberService {

    MemberResponseDTO.UserInfoDTO getUserInfo(String kakaoId);

    MemberResponseDTO.UserPreferencesDTO setUserPreferences(String kakaoId, MemberRequestDTO.UserPreferencesDTO request);

    MemberResponseDTO.UserScrapsDTO getUserScraps(String kakaoId);

    MemberResponseDTO.OnboardingStatusDTO checkOnboardingStatus(String kakaoId);
}
