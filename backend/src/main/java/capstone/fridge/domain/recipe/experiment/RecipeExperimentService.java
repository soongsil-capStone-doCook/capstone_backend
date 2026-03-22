package capstone.fridge.domain.recipe.experiment;

import capstone.fridge.domain.recipe.agent.AgentPlanner;
import capstone.fridge.domain.recipe.agent.AgentTools;
import capstone.fridge.domain.recipe.agent.RecipeAgentService;
import capstone.fridge.domain.recipe.agent.dto.AgentDtos;
import capstone.fridge.domain.recipe.application.RecipeService;
import capstone.fridge.domain.recipe.dto.RecipeRequestDTO;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import capstone.fridge.domain.recipe.domain.entity.Recipe;
import capstone.fridge.domain.recipe.domain.repository.RecipeRepository;
import capstone.fridge.domain.recipe.experiment.dto.ExperimentDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agentic Hybrid RAG 실험용 서비스.
 * - DENSE / HYBRID / HYBRID_RERANK / HYBRID_RERANK_FUSION / AGENTIC 모드별 retrieval
 * - Query Rewriting 단독 (Experiment 3)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeExperimentService {

    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;
    private final AgentPlanner agentPlanner;
    private final AgentTools agentTools;
    private final RecipeAgentService recipeAgentService;

    /**
     * 지정 모드로 retrieval 수행 후 recipe id 목록(순서 유지) 및 부가 정보 반환.
     */
    public ExperimentDtos.RetrieveRes retrieve(ExperimentDtos.RetrieveReq req) {
        String mode = req.getMode() != null ? req.getMode().toUpperCase() : ExperimentDtos.MODE_HYBRID;
        String query = req.getQuery() != null ? req.getQuery().trim() : "";
        int topK = req.getTopK() != null && req.getTopK() > 0 ? req.getTopK() : 50;
        Long memberId = req.getMemberId() != null ? req.getMemberId() : 1L;
        String searchQuery = (req.getRewrittenQuery() != null && !req.getRewrittenQuery().isBlank())
                ? req.getRewrittenQuery().trim()
                : query;

        if (query.isBlank()) {
            return new ExperimentDtos.RetrieveRes(List.of(), List.of(), null);
        }

        switch (mode) {
            case ExperimentDtos.MODE_DENSE -> {
                List<RecipeResponseDTO.RecipeDTO> list = recipeService.searchRecipeDenseOnly(memberId, searchQuery, topK);
                return new ExperimentDtos.RetrieveRes(
                        list.stream().map(RecipeResponseDTO.RecipeDTO::getRecipeId).collect(Collectors.toList()),
                        List.of(),
                        null
                );
            }
            case ExperimentDtos.MODE_HYBRID -> {
                RecipeRequestDTO.SearchRecipeDTO searchReq = new RecipeRequestDTO.SearchRecipeDTO();
                searchReq.setKeyword(searchQuery);
                searchReq.setExcludeAllergy(true);
                List<RecipeResponseDTO.RecipeDTO> list = recipeService.searchRecipeHybrid(memberId, searchReq, topK);
                return new ExperimentDtos.RetrieveRes(
                        list.stream().map(RecipeResponseDTO.RecipeDTO::getRecipeId).collect(Collectors.toList()),
                        List.of(),
                        null
                );
            }
            case ExperimentDtos.MODE_HYBRID_RERANK -> {
                RecipeRequestDTO.SearchRecipeDTO searchReq = new RecipeRequestDTO.SearchRecipeDTO();
                searchReq.setKeyword(searchQuery);
                searchReq.setExcludeAllergy(true);
                int candidateK = Math.max(topK, 50);
                List<RecipeResponseDTO.RecipeDTO> candidates = recipeService.searchRecipeHybrid(memberId, searchReq, candidateK);
                List<RecipeResponseDTO.RecipeDTO> reranked = agentTools.rerankTool(query, candidates, topK);
                return new ExperimentDtos.RetrieveRes(
                        reranked.stream().map(RecipeResponseDTO.RecipeDTO::getRecipeId).collect(Collectors.toList()),
                        List.of(),
                        null
                );
            }
            case ExperimentDtos.MODE_HYBRID_RERANK_FUSION -> {
                RecipeRequestDTO.SearchRecipeDTO searchReq = new RecipeRequestDTO.SearchRecipeDTO();
                searchReq.setKeyword(searchQuery);
                searchReq.setExcludeAllergy(true);
                int candidateK = Math.max(topK, 50);
                List<RecipeResponseDTO.RecipeDTO> candidates = recipeService.searchRecipeHybrid(memberId, searchReq, candidateK);
                List<RecipeResponseDTO.RecipeDTO> reranked = agentTools.rerankTool(query, candidates, candidateK);
                List<Long> fusedIds = applyScoreFusion(candidates, reranked, topK);
                return new ExperimentDtos.RetrieveRes(
                        fusedIds,
                        List.of(),
                        null
                );
            }
            case ExperimentDtos.MODE_AGENTIC -> {
                AgentDtos.AgentAskRes agentRes = recipeAgentService.ask(memberId, query);
                List<Long> ids = agentRes.getRecipes().stream()
                        .limit(topK)
                        .map(RecipeResponseDTO.RecipeDTO::getRecipeId)
                        .collect(Collectors.toList());
                return new ExperimentDtos.RetrieveRes(
                        ids,
                        agentRes.getRewrittenQueries() != null ? agentRes.getRewrittenQueries() : List.of(),
                        agentRes.getAnswer()
                );
            }
            default -> {
                RecipeRequestDTO.SearchRecipeDTO searchReq = new RecipeRequestDTO.SearchRecipeDTO();
                searchReq.setKeyword(searchQuery);
                searchReq.setExcludeAllergy(true);
                List<RecipeResponseDTO.RecipeDTO> list = recipeService.searchRecipeHybrid(memberId, searchReq, topK);
                return new ExperimentDtos.RetrieveRes(
                        list.stream().map(RecipeResponseDTO.RecipeDTO::getRecipeId).collect(Collectors.toList()),
                        List.of(),
                        null
                );
            }
        }
    }

    /**
     * Query Rewriting만 수행 (Experiment 3: Original vs Rewritten 비교 시 사용).
     */
    public ExperimentDtos.RewriteRes rewrite(ExperimentDtos.RewriteReq req) {
        String query = req.getQuery() != null ? req.getQuery().trim() : "";
        if (query.isBlank()) {
            return new ExperimentDtos.RewriteRes(List.of());
        }
        AgentDtos.PlanResult plan = agentPlanner.plan(query);
        List<String> rewritten = plan.getRewrittenQueries() != null && !plan.getRewrittenQueries().isEmpty()
                ? plan.getRewrittenQueries()
                : List.of(query);
        return new ExperimentDtos.RewriteRes(rewritten);
    }

    /**
     * Score Fusion: final_score(d) = α × (1/(k+hybrid_rank)) + (1−α) × (1/(k+rerank_rank))
     * Hybrid 순위와 Rerank 순위를 rank 기반 점수로 융합 후 정렬해 상위 topK개 id 반환.
     */
    private static final int RRF_K = 60;
    private static final double FUSION_ALPHA = 0.5;

    private List<Long> applyScoreFusion(List<RecipeResponseDTO.RecipeDTO> candidates,
                                        List<RecipeResponseDTO.RecipeDTO> reranked,
                                        int topK) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        Map<Long, Integer> hybridRankById = new LinkedHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            hybridRankById.put(candidates.get(i).getRecipeId(), i + 1);
        }
        Map<Long, Integer> rerankRankById = new LinkedHashMap<>();
        for (int i = 0; i < reranked.size(); i++) {
            rerankRankById.put(reranked.get(i).getRecipeId(), i + 1);
        }
        int missingRank = RRF_K + 1;
        return candidates.stream()
                .map(r -> {
                    long id = r.getRecipeId();
                    int hybridRank = hybridRankById.getOrDefault(id, missingRank);
                    int rerankRank = rerankRankById.getOrDefault(id, missingRank);
                    double sHybrid = 1.0 / (RRF_K + hybridRank);
                    double sRerank = 1.0 / (RRF_K + rerankRank);
                    double fusion = FUSION_ALPHA * sHybrid + (1.0 - FUSION_ALPHA) * sRerank;
                    return new Object[] { id, fusion };
                })
                .sorted(Comparator.<Object[], Double>comparing(a -> (Double) a[1]).reversed())
                .limit(topK)
                .map(a -> (Long) a[0])
                .collect(Collectors.toList());
    }

    /** 라벨링용: DB 전체 레시피 id + title 반환 (relevant_doc_ids 채울 때 참고) */
    public ExperimentDtos.RecipeListRes listAllRecipes() {
        List<Recipe> all = recipeRepository.findAll();
        List<ExperimentDtos.RecipeItem> items = all.stream()
                .map(r -> new ExperimentDtos.RecipeItem(r.getId(), r.getTitle()))
                .collect(Collectors.toList());
        return new ExperimentDtos.RecipeListRes(items);
    }
}
