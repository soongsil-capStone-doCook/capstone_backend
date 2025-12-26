package capstone.fridge.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class MemberResponseDTO {

    @Builder
    @Getter
    @AllArgsConstructor
    public static class UserInfoDTO {
        private Long memberId;
        private String nickname;
        private String email;
        private String profileImageUrl;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    public static class UserPreferencesDTO {
        private List<String> allergies;
        private List<String> dislikes;
        private String age;
        private String gender;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    public static class UserScrapsDTO {
        private List<ScrapRecipeDTO> scrapList;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    public static class ScrapRecipeDTO {
        private Long recipeId;
        private String title;
        private String thumbnailUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingStatusDTO {
        private boolean isOnboarded; // true: 정보 입력 완료, false: 미입력
        private Long memberId;       // 회원 고유 ID (PK)
    }
}
