package capstone.fridge.domain.recipe.agent;

import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;

import java.util.List;

/**
 * Agent Generator: Retrieval(및 Re-ranking) 결과를 바탕으로 최종 자연어 응답 생성.
 */
public interface AgentGenerator {

    /**
     * @param userQuery 사용자 원본 질의
     * @param recipes 참조된 레시피 목록 (이미 순서 적용됨)
     * @return 최종 답변 텍스트
     */
    String generate(String userQuery, List<RecipeResponseDTO.RecipeDTO> recipes);
}
