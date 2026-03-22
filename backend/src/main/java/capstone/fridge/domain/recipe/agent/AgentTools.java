package capstone.fridge.domain.recipe.agent;

import capstone.fridge.domain.recipe.agent.dto.AgentDtos;
import capstone.fridge.domain.recipe.application.RecipeService;
import capstone.fridge.domain.recipe.dto.RecipeRequestDTO;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import capstone.fridge.global.client.FastApiClient;
import capstone.fridge.global.client.dto.FastApiRerankDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent Tools: Retrieval(Hybrid Search) / Re-ranking 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTools {

    private static final int RETRIEVAL_TOP_K = 30;
    private static final int RERANK_TOP_K = 5;

    private final RecipeService recipeService;
    private final FastApiClient fastApiClient;

    /**
     * Retrieval Tool: rewritten query로 Hybrid Retrieval 수행.
     */
    public List<RecipeResponseDTO.RecipeDTO> retrievalTool(Long memberId, String query, Boolean excludeAllergy) {
        RecipeRequestDTO.SearchRecipeDTO request = new RecipeRequestDTO.SearchRecipeDTO();
        request.setKeyword(query);
        request.setExcludeAllergy(excludeAllergy != null ? excludeAllergy : true);

        List<RecipeResponseDTO.RecipeDTO> list = recipeService.searchRecipeHybrid(memberId, request);
        return list.size() > RETRIEVAL_TOP_K ? list.subList(0, RETRIEVAL_TOP_K) : list;
    }

    /**
     * Re-ranking Tool: Top-K 결과를 Cross-Encoder로 재정렬.
     * FastAPI /rerank 호출 후 ranked_ids 순서대로 recipes 재배치.
     */
    public List<RecipeResponseDTO.RecipeDTO> rerankTool(String query, List<RecipeResponseDTO.RecipeDTO> recipes) {
        return rerankTool(query, recipes, RERANK_TOP_K);
    }

    /**
     * 실험용: 재정렬 후 상위 maxResults개 반환.
     */
    public List<RecipeResponseDTO.RecipeDTO> rerankTool(String query, List<RecipeResponseDTO.RecipeDTO> recipes, int maxResults) {
        if (query == null || recipes == null || recipes.isEmpty()) {
            return recipes;
        }

        List<FastApiRerankDtos.RerankDoc> docs = recipes.stream()
                .map(r -> new FastApiRerankDtos.RerankDoc(
                        r.getRecipeId(),
                        buildRecipeText(r)
                ))
                .toList();

        FastApiRerankDtos.RerankReq req = new FastApiRerankDtos.RerankReq(query, docs);
        FastApiRerankDtos.RerankRes res;
        try {
            res = fastApiClient.rerank(req);
        } catch (Exception e) {
            log.warn("Rerank API failed, returning original order", e);
            return recipes;
        }

        if (res == null || res.ranked_ids() == null || res.ranked_ids().isEmpty()) {
            return recipes;
        }

        Map<Long, RecipeResponseDTO.RecipeDTO> byId = recipes.stream()
                .collect(Collectors.toMap(RecipeResponseDTO.RecipeDTO::getRecipeId, r -> r, (a, b) -> a, LinkedHashMap::new));

        List<RecipeResponseDTO.RecipeDTO> ordered = new ArrayList<>();
        for (Long id : res.ranked_ids()) {
            RecipeResponseDTO.RecipeDTO r = byId.get(id);
            if (r != null) ordered.add(r);
        }
        // ranked_ids에 없는 항목은 뒤에 붙임
        for (RecipeResponseDTO.RecipeDTO r : recipes) {
            if (!ordered.contains(r)) ordered.add(r);
        }

        return ordered.size() > maxResults ? ordered.subList(0, maxResults) : ordered;
    }

    private static String buildRecipeText(RecipeResponseDTO.RecipeDTO r) {
        StringBuilder sb = new StringBuilder();
        if (r.getTitle() != null) sb.append(r.getTitle()).append(" ");
        if (r.getDescription() != null) sb.append(r.getDescription()).append(" ");
        if (r.getIngredients() != null) {
            r.getIngredients().forEach(i -> sb.append(i.getName()).append(" "));
        }
        return sb.toString().trim();
    }
}
