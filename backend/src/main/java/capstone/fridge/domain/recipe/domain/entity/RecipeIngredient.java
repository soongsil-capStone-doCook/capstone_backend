package capstone.fridge.domain.recipe.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recipe_ingredient")
public class RecipeIngredient {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    // 만개의 레시피 '[재료]', '[양념]' 구분 텍스트
    private String bundleName;

    @Column(nullable = false)
    private String name; // 재료명

    private String amount; // 계량 (예: 1/2모)

    @Builder
    public RecipeIngredient(Recipe recipe, String bundleName, String name, String amount) {
        this.recipe = recipe;
        this.bundleName = bundleName;
        this.name = name;
        this.amount = amount;
    }
}
