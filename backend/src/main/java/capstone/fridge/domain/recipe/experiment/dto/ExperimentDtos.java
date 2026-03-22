package capstone.fridge.domain.recipe.experiment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agentic Hybrid RAG 실험용 요청/응답 DTO
 */
public class ExperimentDtos {

    public static final String MODE_DENSE = "DENSE";
    public static final String MODE_HYBRID = "HYBRID";
    public static final String MODE_HYBRID_RERANK = "HYBRID_RERANK";
    /** Score Fusion: α * Hybrid 순위점수 + (1−α) * Rerank 순위점수 로 융합 후 정렬 */
    public static final String MODE_HYBRID_RERANK_FUSION = "HYBRID_RERANK_FUSION";
    public static final String MODE_AGENTIC = "AGENTIC";

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrieveReq {
        /** DENSE | HYBRID | HYBRID_RERANK | HYBRID_RERANK_FUSION | AGENTIC */
        private String mode;
        private String query;
        /** 상위 k개 (기본 50) */
        private Integer topK;
        /** 미지정 시 1L 사용 */
        private Long memberId;
        /** Experiment 3: 재작성 쿼리로 검색할 때 지정 (없으면 query 사용) */
        private String rewrittenQuery;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrieveRes {
        /** 검색 결과 순서대로 recipe id 목록 */
        private List<Long> recipeIds;
        /** Query Rewriting 결과 (AGENTIC 또는 rewrite 호출 시) */
        private List<String> rewrittenQueries;
        /** AGENTIC 모드일 때만 생성된 답변 */
        private String answer;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewriteReq {
        private String query;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewriteRes {
        @JsonProperty("rewritten_queries")
        private List<String> rewrittenQueries;
    }

    /** 라벨링용: 전체 레시피 id + title */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeItem {
        private Long recipeId;
        private String title;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeListRes {
        private List<RecipeItem> recipes;
    }
}
