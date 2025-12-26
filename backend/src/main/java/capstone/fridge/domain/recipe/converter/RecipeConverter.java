package capstone.fridge.domain.recipe.converter;

import capstone.fridge.domain.recipe.domain.entity.Recipe;
import capstone.fridge.domain.recipe.domain.entity.RecipeStep;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeConverter {

    private static final String S3_BASE_URL = "https://capstone-fridge.s3.ap-northeast-2.amazonaws.com/recipes/";

    // 1. 부족한 재료가 없는 경우 (보유 재료 기반 추천용)
    public static RecipeResponseDTO.RecipeDTO toRecipeDTO(Recipe recipe) {
        return toRecipeDTO(recipe, Collections.emptyList());
    }

    // 2. 부족한 재료가 있는 경우 (부족한 재료 기반 추천용)
    public static RecipeResponseDTO.RecipeDTO toRecipeDTO(Recipe recipe, List<String> missingIngredients) {

        // 이미지 URL 생성
        String s3Url = S3_BASE_URL + recipe.getId() + "/main.png";

        return RecipeResponseDTO.RecipeDTO.builder()
                .recipeId(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .mainImage(s3Url)
                .difficulty(recipe.getDifficulty())
                .cookTime(recipe.getCookTime())
                .missingIngredients(missingIngredients)
                .build();
    }

    public static RecipeResponseDTO.RecipeInfoDTO toRecipeInfoDTO(Recipe recipe) {

        // 1. 재료 리스트 변환
        List<RecipeResponseDTO.IngredientDTO> ingredientDTOs = recipe.getIngredients().stream()
                .map(ing -> RecipeResponseDTO.IngredientDTO.builder()
                        .name(ing.getName())
                        .amount(ing.getAmount())
                        .bundle(ing.getBundleName())
                        .build())
                .collect(Collectors.toList());

        // 2. 조리 순서 리스트 변환 (순서대로 정렬 필요)
        List<RecipeResponseDTO.StepDTO> stepDTOs = recipe.getSteps().stream()
                .sorted(Comparator.comparing(RecipeStep::getStepOrder)) // 1, 2, 3... 순서 정렬
                .map(step -> {
                    // Step 이미지 URL 생성 규칙 (파이썬 크롤러 로직 반영)
                    String stepImageUrl = S3_BASE_URL + recipe.getId() + "/steps/" + step.getStepOrder() + ".png";

                    return RecipeResponseDTO.StepDTO.builder()
                            .order(step.getStepOrder())
                            .content(step.getContent())
                            .image(stepImageUrl)
                            .build();
                })
                .collect(Collectors.toList());

        // 3. 최종 DTO 조립
        return RecipeResponseDTO.RecipeInfoDTO.builder()
                .recipeId(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .mainImage(S3_BASE_URL + recipe.getId() + "/main.png") // 메인 이미지
                .servings(recipe.getServings())
                .cookTime(recipe.getCookTime())
                .difficulty(recipe.getDifficulty())
                .ingredients(ingredientDTOs)
                .steps(stepDTOs)
                .build();
    }
}
