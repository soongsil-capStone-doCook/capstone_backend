package capstone.fridge.domain.recipe.application;

import capstone.fridge.domain.recipe.dto.RecipeRequestDTO;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;

import java.util.List;

public interface RecipeService {

    List<RecipeResponseDTO.RecipeDTO> recommendRecipes(String kakaoId);

    List<RecipeResponseDTO.RecipeDTO> recommendMissingRecipes(String kakaoId);

    List<RecipeResponseDTO.RecipeDTO> recommendScrapsRecipes(String kakaoId);

    RecipeResponseDTO.RecipeInfoDTO getRecipe(Long recipeId);

    List<RecipeResponseDTO.RecipeDTO> searchRecipe(RecipeRequestDTO.SearchRecipeDTO request);

    RecipeResponseDTO.RecipeScrapDTO scrapRecipe(Long recipeId, String kakaoId);

    void deleteScrapRecipe(Long recipeId, String kakaoId);
}
