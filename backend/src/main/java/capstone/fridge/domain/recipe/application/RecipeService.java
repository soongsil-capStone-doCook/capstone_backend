package capstone.fridge.domain.recipe.application;

import capstone.fridge.domain.recipe.dto.RecipeRequestDTO;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;

import java.util.List;

public interface RecipeService {

    List<RecipeResponseDTO.RecipeDTO> recommendRecipes(Long memberId);

    List<RecipeResponseDTO.RecipeDTO> recommendMissingRecipes(Long memberId);

    List<RecipeResponseDTO.RecipeDTO> recommendScrapsRecipes(Long memberId);

    RecipeResponseDTO.RecipeInfoDTO getRecipe(Long recipeId, Long memberId);

    List<RecipeResponseDTO.RecipeDTO> searchRecipe(Long memberId, RecipeRequestDTO.SearchRecipeDTO request);

    RecipeResponseDTO.RecipeScrapDTO scrapRecipe(Long recipeId, Long memberId);

    void deleteScrapRecipe(Long recipeId, Long memberId);

    List<RecipeResponseDTO.RecipeDTO> recommendRecipesHybrid(Long memberId);

    List<RecipeResponseDTO.RecipeDTO> recommendMissingRecipesHybrid(Long memberId);

    List<RecipeResponseDTO.RecipeDTO> searchRecipeHybrid(Long memberId, RecipeRequestDTO.SearchRecipeDTO request);

    /** 실험용: 상위 topK개만 반환 */
    List<RecipeResponseDTO.RecipeDTO> searchRecipeHybrid(Long memberId, RecipeRequestDTO.SearchRecipeDTO request, int topK);

    /** 실험용: Dense만 사용한 검색 (Ablation Baseline) */
    List<RecipeResponseDTO.RecipeDTO> searchRecipeDenseOnly(Long memberId, String queryText, int topK);
}
