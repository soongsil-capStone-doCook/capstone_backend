package capstone.fridge.domain.recipe.domain.repository;

import capstone.fridge.domain.recipe.domain.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Query("SELECT r FROM Recipe r " +
            "WHERE NOT EXISTS (" +
            "    SELECT ri FROM RecipeIngredient ri " +
            "    WHERE ri.recipe = r " +
            "    AND ri.name IN :excludedIngredients" +
            ") " +
            "AND NOT EXISTS (" +
            "    SELECT ri FROM RecipeIngredient ri " +
            "    WHERE ri.recipe = r " +
            "    AND ri.name NOT IN :fridgeIngredients" +
            ")")
    List<Recipe> findCookableRecipes(
            @Param("fridgeIngredients") List<String> fridgeIngredients,
            @Param("excludedIngredients") List<String> excludedIngredients
    );
}
