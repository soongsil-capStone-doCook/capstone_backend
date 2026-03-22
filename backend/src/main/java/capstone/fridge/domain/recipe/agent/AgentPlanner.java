package capstone.fridge.domain.recipe.agent;

import capstone.fridge.domain.recipe.agent.dto.AgentDtos;

/**
 * Agent Planner: 사용자 질의를 Query Rewriting 하고, Retrieval / Re-ranking 도구 호출 여부를 판단.
 */
public interface AgentPlanner {

    /**
     * @param userQuery 사용자 원본 질의
     * @return rewritten queries + call_retrieval, call_rerank
     */
    AgentDtos.PlanResult plan(String userQuery);
}
