package capstone.fridge.domain.scrap.domain.entity;

import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.model.entity.BaseTimeEntity;
import capstone.fridge.domain.recipe.domain.entity.Recipe;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recipe_scrap",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "recipe_id"}) // 중복 찜 방지
        })
public class RecipeScrap extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Builder
    public RecipeScrap(Member member, Recipe recipe) {
        this.member = member;
        this.recipe = recipe;
    }
}
