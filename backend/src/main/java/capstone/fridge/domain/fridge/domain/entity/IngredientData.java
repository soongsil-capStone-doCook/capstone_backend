package capstone.fridge.domain.fridge.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ingredient_data")
public class IngredientData {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // 재료명 (예: 양파, 계란)

    private String category; // (채소, 유제품...)

    public IngredientData(String name, String category) {
        this.name = name;
        this.category = category;
    }
}