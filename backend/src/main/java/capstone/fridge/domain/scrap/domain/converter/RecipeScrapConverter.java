package capstone.fridge.domain.scrap.domain.converter;

import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.recipe.domain.entity.Recipe;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import capstone.fridge.domain.scrap.domain.entity.RecipeScrap;

public class RecipeScrapConverter {

    public static RecipeScrap toRecipeScrap(Member member, Recipe recipe) {
        return RecipeScrap.builder()
                .member(member)
                .recipe(recipe)
                .build();
    }

    public static RecipeResponseDTO.RecipeScrapDTO toRecipeScrapDTO(RecipeScrap recipeScrap) {
        return RecipeResponseDTO.RecipeScrapDTO.builder()
                .scrapId(recipeScrap.getId())
                .recipeId(recipeScrap.getRecipe().getId())
                .memberId(recipeScrap.getMember().getId())
                .createdAt(recipeScrap.getCreatedAt())
                .build();
    }
}
