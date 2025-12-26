package capstone.fridge.domain.recipe.domain.repository;

import capstone.fridge.domain.recipe.domain.entity.RecipeStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeStepRepository extends JpaRepository<RecipeStep, Long> {
}
