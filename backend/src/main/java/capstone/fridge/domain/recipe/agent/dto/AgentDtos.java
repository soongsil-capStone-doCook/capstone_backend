package capstone.fridge.domain.recipe.agent.dto;

import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agentic RAG 요청/응답 및 Planner 결과 DTO
 */
public class AgentDtos {

    /** 사용자 질의 → Agent ask 요청 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentAskReq {
        private String query;
    }

    /** Agent 최종 응답 */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentAskRes {
        private String answer;                    // LLM 생성 최종 답변
        private List<RecipeResponseDTO.RecipeDTO> recipes;  // 참조된 레시피 목록
        private List<String> rewrittenQueries;    // Query Rewriting 결과
        private boolean usedRetrieval;
        private boolean usedRerank;
    }

    /** Planner LLM 출력: Query Rewriting + 도구 호출 여부 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanResult {
        @JsonProperty("rewritten_queries")
        private List<String> rewrittenQueries;

        @JsonProperty("call_retrieval")
        private boolean callRetrieval;

        @JsonProperty("call_rerank")
        private boolean callRerank;
    }

    /** Re-ranking 요청 시 레시피 요약 (Generator/요약용 아님, rerank API 전달용) */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeSummaryForRerank {
        private Long recipeId;
        private String text;  // title + description 등 검색 스니펫
    }
}
