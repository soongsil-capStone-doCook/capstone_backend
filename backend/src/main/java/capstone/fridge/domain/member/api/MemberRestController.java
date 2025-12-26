package capstone.fridge.domain.member.api;

import capstone.fridge.domain.member.application.MemberService;
import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.member.domain.repository.MemberRepository;
import capstone.fridge.domain.member.dto.MemberRequestDTO;
import capstone.fridge.domain.member.dto.MemberResponseDTO;
import capstone.fridge.global.common.response.BaseResponse;
//import capstone.fridge.global.config.security.PrincipalDetails;
import capstone.fridge.global.error.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MemberRestController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회 API", description = "사용자의 정보에 대한 정보를 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "MEMBER_200", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<MemberResponseDTO.UserInfoDTO> getUserInfo(
            @RequestParam String kakaoId
    ) {
        MemberResponseDTO.UserInfoDTO result = memberService.getUserInfo(kakaoId);
        return BaseResponse.onSuccess(SuccessStatus.MEMBER_INFO, result);
    }

    @PatchMapping("/me/preferences")
    @Operation(summary = "건강 정보 및 기호 수정 API", description = "사용자의 알레르기 및 기호 식품에 대한 정보를 수정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "MEMBER_200", description = "OK, 성공적으로 수정되었습니다.")
    })
    public BaseResponse<MemberResponseDTO.UserPreferencesDTO> setUserPreferences(
            @RequestParam String kakaoId,
            @RequestBody MemberRequestDTO.UserPreferencesDTO preferences
    ) {
        MemberResponseDTO.UserPreferencesDTO result = memberService.setUserPreferences(kakaoId, preferences);
        return BaseResponse.onSuccess(SuccessStatus.MEMBER_PREFERENCE, result);
    }

    @GetMapping("/me/scraps")
    @Operation(summary = "찜한 레시피 목록 조회 API", description = "사용자가 찜한 레시피 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "MEMBER_200", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<MemberResponseDTO.UserScrapsDTO> getUserScraps(
            @RequestParam String kakaoId
    ) {
        MemberResponseDTO.UserScrapsDTO result = memberService.getUserScraps(kakaoId);
        return BaseResponse.onSuccess(SuccessStatus.MEMBER_SCRAPS, result);
    }

    @GetMapping("/onboarding/check")
    @Operation(summary = "온보딩 여부 확인 API", description = "사용자가 나이, 성별 등 필수 정보를 입력했는지 확인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "MEMBER_200", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<MemberResponseDTO.OnboardingStatusDTO> checkOnboarding(
            @RequestParam String kakaoId
    ) {
        MemberResponseDTO.OnboardingStatusDTO result = memberService.checkOnboardingStatus(kakaoId);

        return BaseResponse.onSuccess(SuccessStatus.OK, result);
    }
}
