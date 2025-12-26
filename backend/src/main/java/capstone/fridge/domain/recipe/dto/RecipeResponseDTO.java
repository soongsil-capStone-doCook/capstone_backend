package capstone.fridge.domain.recipe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class RecipeResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeDTO {
        private Long recipeId;
        private String title;
        private String description;
        private String mainImage; // S3 URL
        private String cookTime;
        private String difficulty;
        private Integer servings;
        private List<String> missingIngredients;
    }

    // 상세 조회용 DTO
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeInfoDTO {
        private Long recipeId;
        private String title;
        private String description;
        private String mainImage;
        private String servings;   // 인분
        private String cookTime;   // 조리 시간
        private String difficulty; // 난이도
        private List<IngredientDTO> ingredients;
        private List<StepDTO> steps;
    }

    // 재료 정보 DTO
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientDTO {
        private String name;   // 재료명 (예: 김치)
        private String amount; // 계량 (예: 1/2포기)
        private String bundle; // 분류 (예: [재료], [양념]) - 선택 사항
    }

    // 조리 순서 DTO
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDTO {
        private Integer order;   // 순서 (1, 2, 3...)
        private String content;  // 설명
        private String image;    // 단계별 이미지 URL
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeScrapDTO {
        private Long scrapId;
        private Long recipeId;
        private Long memberId;
        private LocalDateTime createdAt;
    }
}
