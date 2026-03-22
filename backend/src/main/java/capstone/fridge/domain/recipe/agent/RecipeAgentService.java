package capstone.fridge.domain.recipe.agent;

import capstone.fridge.domain.recipe.agent.dto.AgentDtos;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Agentic RAG 오케스트레이터: Planner → Tool(Retrieval / Re-ranking) → Generator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeAgentService {

    private final AgentPlanner agentPlanner;
    private final AgentTools agentTools;
    private final AgentGenerator agentGenerator;

    /**
     * 사용자 질의에 대해 Query Rewriting → (선택) Retrieval → (선택) Re-ranking → 최종 답변 생성
     */
    @Transactional(readOnly = true)
    public AgentDtos.AgentAskRes ask(Long memberId, String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return AgentDtos.AgentAskRes.builder()
                    .answer("질문을 입력해 주세요.")
                    .recipes(List.of())
                    .rewrittenQueries(List.of())
                    .usedRetrieval(false)
                    .usedRerank(false)
                    .build();
        }

        // 1. Planner: Query Rewriting + 도구 호출 여부
        AgentDtos.PlanResult plan = agentPlanner.plan(userQuery);
        List<String> rewrittenQueries = plan.getRewrittenQueries() != null ? plan.getRewrittenQueries() : List.of(userQuery);
        String searchQuery = rewrittenQueries.isEmpty() ? userQuery : rewrittenQueries.get(0);

        List<RecipeResponseDTO.RecipeDTO> recipes = new ArrayList<>();
        boolean usedRetrieval = false;
        boolean usedRerank = false;

        // 2. Tool: Retrieval (Hybrid)
        if (plan.isCallRetrieval()) {
            recipes = agentTools.retrievalTool(memberId, searchQuery, true);
            usedRetrieval = true;
        }

        // 3. Tool: Re-ranking (선택)
        if (plan.isCallRerank() && !recipes.isEmpty()) {
            recipes = agentTools.rerankTool(userQuery, recipes);
            usedRerank = true;
        }

        // 4. Generator: 최종 응답 생성
        String answer = agentGenerator.generate(userQuery, recipes);

        return AgentDtos.AgentAskRes.builder()
                .answer(answer)
                .recipes(recipes)
                .rewrittenQueries(rewrittenQueries)
                .usedRetrieval(usedRetrieval)
                .usedRerank(usedRerank)
                .build();
    }
}
