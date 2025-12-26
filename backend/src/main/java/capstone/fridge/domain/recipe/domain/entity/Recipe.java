package capstone.fridge.domain.recipe.domain.entity;

import capstone.fridge.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recipe", indexes = {
        @Index(name = "idx_original_id", columnList = "originalRecipeId") // 크롤링 중복 방지 인덱스
})
public class Recipe extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long id;

    // 만개의 레시피 URL 상의 ID (중복 크롤링 방지용)
    @Column(unique = true, nullable = false)
    private Long originalRecipeId;

    @Column(nullable = false)
    private String title; // 요리명

    @Column(columnDefinition = "TEXT")
    private String description; // 요리 설명

    // 메타 정보
    private String servings; // 인분 (예: 2인분)
    private String cookTime; // 시간 (예: 30분이내)
    private String difficulty; // 난이도 (예: 아무나)

    // 정렬을 위한 외부 데이터 (선택사항)
    private Long externalViews = 0L; // 조회수
    private Long externalScraps = 0L; // 스크랩수

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeStep> steps = new ArrayList<>();

    @Builder
    public Recipe(Long originalRecipeId, String title, String description,
                  String servings, String cookTime, String difficulty, Long externalViews, Long externalScraps) {
        this.originalRecipeId = originalRecipeId;
        this.title = title;
        this.description = description;
        this.servings = servings;
        this.cookTime = cookTime;
        this.difficulty = difficulty;
        this.externalViews = (externalViews != null) ? externalViews : 0L;
        this.externalScraps = (externalScraps != null) ? externalScraps : 0L;
    }

    public void increaseViewCount() {
        if (this.externalViews == null) {
            this.externalViews = 0L;
        }
        this.externalViews++;
    }

    public void increaseScrapsCount() {
        if (this.externalScraps == null) {
            this.externalScraps = 0L;
        }
        this.externalScraps++;
    }

    public void decreaseScrapsCount() {
        if (this.externalScraps == null) {
            this.externalScraps = 0L;
        }
        if (this.externalScraps > 0) {
            this.externalScraps--;
        }
    }
}
