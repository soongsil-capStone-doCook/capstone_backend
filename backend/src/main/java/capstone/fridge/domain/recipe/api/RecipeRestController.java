package capstone.fridge.domain.recipe.api;

import capstone.fridge.domain.recipe.application.RecipeService;
import capstone.fridge.domain.recipe.dto.RecipeRequestDTO;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import capstone.fridge.global.common.response.BaseResponse;
import capstone.fridge.global.error.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeRestController {

    private final RecipeService recipeService;

    @GetMapping("/recommend/fridge")
    @Operation(summary = "맞춤 레시피 추천 API", description = "사용자 냉장고의 재료를 기반으로 만들 수 있는 레시피를 추천")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_200", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<List<RecipeResponseDTO.RecipeDTO>> recommendRecipes(
            @RequestParam String kakaoId
    ) {
        List<RecipeResponseDTO.RecipeDTO> result = recipeService.recommendRecipes(kakaoId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, result);
    }

    @GetMapping("/recommend/fridge/missing")
    @Operation(summary = "부족 재료 기반 레시피 추천 API", description = "사용자 냉장고의 재료를 기반으로 조금의 재료를 추가하면 만들 수 있는 레시피를 추천")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_200", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<List<RecipeResponseDTO.RecipeDTO>> recommendMissingRecipes(
            @RequestParam String kakaoId
    ) {
        List<RecipeResponseDTO.RecipeDTO> result = recipeService.recommendMissingRecipes(kakaoId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, result);
    }

    @GetMapping("/recommend/scraps")
    @Operation(summary = "찜한 레시피 기반 레시피 추천 API", description = "사용자가 찜한 레시피를 기반으로 좋아할만 한 레시피를 추천")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_202", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<List<RecipeResponseDTO.RecipeDTO>> recommendScrapsRecipes(
            @RequestParam String kakaoId
    ) {
        List<RecipeResponseDTO.RecipeDTO> result = recipeService.recommendScrapsRecipes(kakaoId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE, result);
    }

    @GetMapping("/recommend/{recipeId}")
    @Operation(summary = "레시피 상세 조회 API", description = "사용자가 누른 특정 레시피의 상세 내용을 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_203", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<RecipeResponseDTO.RecipeInfoDTO> getRecipe(
            @PathVariable Long recipeId
    ) {
        RecipeResponseDTO.RecipeInfoDTO result = recipeService.getRecipe(recipeId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE_INFO, result);
    }

    @GetMapping("/recommend/search")
    @Operation(summary = "레시피 검색 API", description = "사용자의 검색 조건에 부합하는 레시피를 반환")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_204", description = "OK, 성공적으로 조회되었습니다.")
    })
    public BaseResponse<List<RecipeResponseDTO.RecipeDTO>> searchRecipe(
            @ModelAttribute RecipeRequestDTO.SearchRecipeDTO request
    ) {
        List<RecipeResponseDTO.RecipeDTO> result = recipeService.searchRecipe(request);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE_FIND, result);
    }

    @PostMapping("/recommend/{recipeId}/scrap")
    @Operation(summary = "레시피 찜하기 API", description = "사용자가 마음에 드는 레시피를 찜")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_205", description = "OK, 성공적으로 찜 되었습니다.")
    })
    public BaseResponse<RecipeResponseDTO.RecipeScrapDTO> scrapRecipe(
            @PathVariable Long recipeId,
            @RequestParam String kakaoId
    ) {
        RecipeResponseDTO.RecipeScrapDTO result = recipeService.scrapRecipe(recipeId, kakaoId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE_SCRAP, result);
    }

    @DeleteMapping("/recommend/{recipeId}/scrap")
    @Operation(summary = "레시피 찜 취소하기 API", description = "사용자가 한 찜을 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "RECIPE_206", description = "OK, 성공적으로 취소되었습니다.")
    })
    public BaseResponse<Void> deleteScrapRecipe(
            @PathVariable Long recipeId,
            @RequestParam String kakaoId
    ) {
        recipeService.deleteScrapRecipe(recipeId, kakaoId);
        return BaseResponse.onSuccess(SuccessStatus.RECIPE_DELETE_SCRAP, null);
    }
}
