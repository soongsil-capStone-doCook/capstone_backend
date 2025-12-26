package capstone.fridge.domain.scrap.domain.repository;

import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.recipe.domain.entity.Recipe;
import capstone.fridge.domain.scrap.domain.entity.RecipeScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecipeScrapRepository extends JpaRepository<RecipeScrap, Long> {

    @Query("SELECT rs FROM RecipeScrap rs JOIN FETCH rs.recipe WHERE rs.member = :member")
    List<RecipeScrap> findAllByMemberWithRecipe(@Param("member") Member member);

    @Query("SELECT s FROM RecipeScrap s JOIN FETCH s.recipe WHERE s.member.id = :memberId")
    List<RecipeScrap> findAllByMemberIdWithRecipe(@Param("memberId") Long memberId);

    Optional<RecipeScrap> findByMemberIdAndRecipeId(Long memberId, Long recipeId);

    boolean existsByMemberAndRecipe(Member member, Recipe recipe);
}
