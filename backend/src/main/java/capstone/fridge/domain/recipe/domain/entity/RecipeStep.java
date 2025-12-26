package capstone.fridge.domain.recipe.domain.entity;

import capstone.fridge.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recipe_step")
public class RecipeStep extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(nullable = false)
    private int stepOrder; // 순서 (1, 2...)

    @Column(columnDefinition = "TEXT")
    private String content; // 설명

    @Column(columnDefinition = "TEXT")
    private String tip; // 요리 팁 (선택)

    @Builder
    public RecipeStep(Recipe recipe, int stepOrder, String content, String tip) {
        this.recipe = recipe;
        this.stepOrder = stepOrder;
        this.content = content;
        this.tip = tip;
    }
}
