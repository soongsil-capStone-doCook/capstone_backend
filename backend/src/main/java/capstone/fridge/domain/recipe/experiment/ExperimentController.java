package capstone.fridge.domain.recipe.experiment;

import capstone.fridge.domain.recipe.experiment.dto.ExperimentDtos;
import capstone.fridge.global.common.response.BaseResponse;
import capstone.fridge.global.error.code.status.SuccessStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Agentic Hybrid RAG 실험용 API.
 * - POST /recipes/experiment/retrieve : 모드별 retrieval → recipeIds, rewrittenQueries, answer
 * - POST /recipes/experiment/rewrite   : Query Rewriting만 수행 (Experiment 3)
 */
@RestController
@RequestMapping("/recipes/experiment")
@RequiredArgsConstructor
public class ExperimentController {

    private final RecipeExperimentService recipeExperimentService;

    @PostMapping("/retrieve")
    public BaseResponse<ExperimentDtos.RetrieveRes> retrieve(@RequestBody ExperimentDtos.RetrieveReq req) {
        ExperimentDtos.RetrieveRes res = recipeExperimentService.retrieve(req);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, res);
    }

    @PostMapping("/rewrite")
    public BaseResponse<ExperimentDtos.RewriteRes> rewrite(@RequestBody ExperimentDtos.RewriteReq req) {
        ExperimentDtos.RewriteRes res = recipeExperimentService.rewrite(req);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, res);
    }

    /** 라벨링용: 전체 레시피 id + title (relevant_doc_ids 채울 때 참고) */
    @GetMapping("/recipes")
    public BaseResponse<ExperimentDtos.RecipeListRes> listRecipes() {
        ExperimentDtos.RecipeListRes res = recipeExperimentService.listAllRecipes();
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, res);
    }
}
