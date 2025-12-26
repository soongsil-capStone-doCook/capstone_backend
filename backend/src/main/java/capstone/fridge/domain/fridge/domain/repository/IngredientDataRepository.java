package capstone.fridge.domain.fridge.domain.repository;

import capstone.fridge.domain.fridge.domain.entity.IngredientData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientDataRepository extends JpaRepository <IngredientData, Long> {
}
