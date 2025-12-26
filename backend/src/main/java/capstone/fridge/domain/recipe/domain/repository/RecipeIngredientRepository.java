package capstone.fridge.domain.recipe.domain.repository;

import capstone.fridge.domain.recipe.domain.entity.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
}
