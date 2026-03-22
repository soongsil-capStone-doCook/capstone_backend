package capstone.fridge.domain.recipe.api;

import capstone.fridge.domain.recipe.agent.dto.AgentDtos;
import capstone.fridge.domain.recipe.agent.RecipeAgentService;
import capstone.fridge.domain.recipe.application.RecipeService;
import capstone.fridge.domain.recipe.dto.RecipeRequestDTO;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import capstone.fridge.global.common.response.BaseResponse;
import capstone.fridge.global.error.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeRestController {

    private final RecipeService recipeService;
    private final RecipeAgentService recipeAgentService;

    @GetMapping("/recommend/fridge")
    @Operation(summary = "맞춤 레시피 추천 API", description = "사용자 냉장고의 재료를 기반으로 만들 수 있는 레시피를 추천")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_200", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<List<RecipeResponseDTO.RecipeDTO>> recommendRecipes(
            @AuthenticationPrincipal Long memberId
    ) {
        List<RecipeResponseDTO.RecipeDTO> result = recipeService.recommendRecipesHybrid(memberId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, result);
    }

    @GetMapping("/recommend/fridge/missing")
    @Operation(summary = "부족 재료 기반 레시피 추천 API", description = "사용자 냉장고의 재료를 기반으로 조금의 재료를 추가하면 만들 수 있는 레시피를 추천")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_200", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<List<RecipeResponseDTO.RecipeDTO>> recommendMissingRecipes(
            @AuthenticationPrincipal Long memberId
    ) {
        List<RecipeResponseDTO.RecipeDTO> result = recipeService.recommendMissingRecipesHybrid(memberId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, result);
    }

    @GetMapping("/recommend/scraps")
    @Operation(summary = "찜한 레시피 기반 레시피 추천 API", description = "사용자가 찜한 레시피를 기반으로 좋아할만 한 레시피를 추천")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_202", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<List<RecipeResponseDTO.RecipeDTO>> recommendScrapsRecipes(
            @AuthenticationPrincipal Long memberId
    ) {
        List<RecipeResponseDTO.RecipeDTO> result = recipeService.recommendScrapsRecipes(memberId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, result);
    }

    @GetMapping("/{recipeId}")
    @Operation(summary = "레시피 상세 조회 API", description = "사용자가 누른 특정 레시피의 상세 내용을 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_203", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<RecipeResponseDTO.RecipeInfoDTO> getRecipe(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long recipeId
    ) {
        RecipeResponseDTO.RecipeInfoDTO result = recipeService.getRecipe(recipeId, memberId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE_INFO, result);
    }

    @GetMapping("/recommend/search")
    @Operation(summary = "레시피 검색 API", description = "사용자의 검색 조건에 부합하는 레시피를 반환")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_204", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<List<RecipeResponseDTO.RecipeDTO>> searchRecipe(
            @AuthenticationPrincipal Long memberId,
            @ModelAttribute RecipeRequestDTO.SearchRecipeDTO request
    ) {
        List<RecipeResponseDTO.RecipeDTO> result = recipeService.searchRecipeHybrid(memberId, request);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE_FIND, result);
    }

    @PostMapping("/{recipeId}/scrap")
    @Operation(summary = "레시피 찜하기 API", description = "사용자가 마음에 드는 레시피를 찜")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_205", description = "OK, 성공적으로 찜 되었습니다.")
    })
    public BaseResponse<RecipeResponseDTO.RecipeScrapDTO> scrapRecipe(
            @PathVariable Long recipeId,
            @AuthenticationPrincipal Long memberId
    ) {
        RecipeResponseDTO.RecipeScrapDTO result = recipeService.scrapRecipe(recipeId, memberId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE_SCRAP, result);
    }

    @DeleteMapping("/{recipeId}/scrap")
    @Operation(summary = "레시피 찜 취소하기 API", description = "사용자가 한 찜을 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_206", description = "OK, 성공적으로 취소되었습니다.")
    })
    public BaseResponse<Void> deleteScrapRecipe(
            @PathVariable Long recipeId,
            @AuthenticationPrincipal Long memberId
    ) {
        recipeService.deleteScrapRecipe(recipeId, memberId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE_DELETE_SCRAP, null);
    }

    @PostMapping("/agent/ask")
    @Operation(summary = "Agentic RAG 레시피 질의 API", description = "Query Rewriting → Hybrid Retrieval → (선택) Re-ranking → 최종 답변 생성")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
    })
    public BaseResponse<AgentDtos.AgentAskRes> agentAsk(
            @AuthenticationPrincipal Long memberId,
            @RequestBody AgentDtos.AgentAskReq req
    ) {
        // 시큐리티 비활성화 시 memberId가 null → 테스트용 기본 1L 사용 (DB에 member id=1 존재해야 함)
        Long effectiveMemberId = memberId != null ? memberId : 1L;
        AgentDtos.AgentAskRes result = recipeAgentService.ask(effectiveMemberId, req.getQuery());
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, result);
    }
}
