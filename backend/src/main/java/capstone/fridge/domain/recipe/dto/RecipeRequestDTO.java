package capstone.fridge.domain.recipe.dto;

import lombok.Getter;
import lombok.Setter;

public class RecipeRequestDTO {

    @Getter
    @Setter
    public static class SearchRecipeDTO {
        private String keyword;        // 검색어 (요리명 또는 재료명)
        private String sort;           // 정렬 기준 (latest, popular, time)
        private Boolean excludeAllergy; // 알레르기 필터 여부 (true/false)
        private String kakaoId;         // 사용자 ID (Security 적용 전 임시)
    }
}
