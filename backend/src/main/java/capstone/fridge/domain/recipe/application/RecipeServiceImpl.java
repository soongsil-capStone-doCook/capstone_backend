package capstone.fridge.domain.recipe.application;

import capstone.fridge.domain.fridge.domain.repository.FridgeIngredientRepository;
import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.member.domain.entity.MemberPreference;
import capstone.fridge.domain.member.domain.repository.MemberPreferenceRepository;
import capstone.fridge.domain.member.domain.repository.MemberRepository;
import capstone.fridge.domain.member.exception.memberException;
import capstone.fridge.domain.recipe.converter.RecipeConverter;
import capstone.fridge.domain.recipe.domain.entity.Recipe;
import capstone.fridge.domain.recipe.domain.entity.RecipeIngredient;
import capstone.fridge.domain.recipe.domain.repository.RecipeRepository;
import capstone.fridge.domain.recipe.dto.RecipeRequestDTO;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import capstone.fridge.domain.recipe.exception.recipeException;
import capstone.fridge.domain.scrap.domain.converter.RecipeScrapConverter;
import capstone.fridge.domain.scrap.domain.entity.RecipeScrap;
import capstone.fridge.domain.scrap.domain.repository.RecipeScrapRepository;
import capstone.fridge.global.error.code.status.ErrorStatus;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.FieldCondition;
import io.qdrant.client.grpc.Points.Match;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final FridgeIngredientRepository fridgeIngredientRepository;
    private final QdrantClient qdrantClient;
    private final EmbeddingService embeddingService;
    private final MemberRepository memberRepository;
    private final MemberPreferenceRepository memberPreferenceRepository;
    private final RecipeScrapRepository recipeScrapRepository;

    @Override
    public List<RecipeResponseDTO.RecipeDTO> recommendRecipes(String kakaoId) {

        // 1. 멤버 검증
        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        // 2. 사용자의 냉장고 재료 가져오기
        List<String> fridgeIngredients = fridgeIngredientRepository.findIngredientNamesByMemberId(member.getId());

        if (fridgeIngredients.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 제외할 재료(알레르기, 기피) 목록 조회
        List<MemberPreference> preferences = memberPreferenceRepository.findAllByMemberId(member.getId());
        List<String> excludedIngredients = preferences.stream()
                .map(MemberPreference::getIngredientName)
                .collect(Collectors.toList());

        // 빈 리스트가 들어가면 SQL IN 절에서 에러가 날 수 있으므로 더미 데이터나 처리 필요
        if (excludedIngredients.isEmpty()) {
            excludedIngredients.add(""); // 매칭되지 않을 임의의 값
        }

        // 4. DB 조회 (냉장고 재료로만 가능한 레시피 검색)
        List<Recipe> cookableRecipes = recipeRepository.findCookableRecipes(fridgeIngredients, excludedIngredients);

        // 5. Converter를 사용하여 DTO 변환 및 반환
        return cookableRecipes.stream()
                .map(RecipeConverter::toRecipeDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeResponseDTO.RecipeDTO> recommendMissingRecipes(String kakaoId) {

        // 1. 멤버 검증
        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        Long memberId = member.getId();

        // 2. 사용자의 냉장고 재료 가져오기
        List<String> ingredients = fridgeIngredientRepository.findIngredientNamesByMemberId(member.getId());

        if (ingredients.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 임베딩 생성 (검색어: 냉장고 재료)
        String queryText = String.join(" ", ingredients);
        List<Float> queryVector = embeddingService.getEmbedding(queryText);

        // 4. 제외할 재료(알레르기, 기피) 목록 조회
        List<MemberPreference> preferences = memberPreferenceRepository.findAllByMemberId(member.getId());
        List<String> excludedIngredients = preferences.stream()
                .map(MemberPreference::getIngredientName)
                .collect(Collectors.toList());

        // 5. Qdrant 필터 생성 (제외 로직)
        Filter.Builder filterBuilder = Filter.newBuilder();

        for (String excluded : excludedIngredients) {
            filterBuilder.addMustNot(Condition.newBuilder()
                    .setField(FieldCondition.newBuilder()
                            .setKey("ingredients") // Qdrant에 저장된 Payload Key
                            .setMatch(Match.newBuilder().setKeyword(excluded).build()) // 정확한 단어 매칭
                            .build())
                    .build());
        }
        Filter filter = filterBuilder.build();

        try {
            // 6. Qdrant 검색 (필터 적용)
            List<Points.ScoredPoint> searchResult = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName("recipes")
                            .setFilter(filter) // [중요] 필터 적용
                            .addAllVector(queryVector)
                            .setLimit(5)
                            .build()
            ).get();

            // 7. 결과 ID 추출
            List<Long> recipeIds = searchResult.stream()
                    .map(point -> Long.parseLong(point.getId().getNum() + ""))
                    .collect(Collectors.toList());

            if (recipeIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 8. MySQL 상세 조회 및 반환
            List<Recipe> recipes = recipeRepository.findAllById(recipeIds);

            return recipes.stream()
                    .map(recipe -> {
                        List<String> recipeIngredientNames = recipe.getIngredients().stream()
                                .map(RecipeIngredient::getName)
                                .collect(Collectors.toList());

                        List<String> missingIngredients = new ArrayList<>(recipeIngredientNames);
                        missingIngredients.removeAll(ingredients);

                        return RecipeConverter.toRecipeDTO(recipe, missingIngredients);
                    })
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant Search Failed: memberId={}", memberId, e);
            throw new recipeException(ErrorStatus._RECIPE_SEARCH_FAIL);
        }
    }

    @Override
    public List<RecipeResponseDTO.RecipeDTO> recommendScrapsRecipes(String kakaoId) {

        // 1. 사용자 검증
        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        Long memberId = member.getId();

        // 2. 사용자가 찜한(Scrap) 레시피 목록 조회
        List<RecipeScrap> scraps = recipeScrapRepository.findAllByMemberIdWithRecipe(memberId);

        if (scraps.isEmpty()) {
            // 찜한 게 없으면 빈 리스트 반환 (또는 랜덤 추천 로직으로 대체 가능)
            return Collections.emptyList();
        }

        // 3. 찜한 레시피들의 ID 목록 (이미 본 건 추천 제외하기 위함)
        List<Long> scrapedRecipeIds = scraps.stream()
                .map(scrap -> scrap.getRecipe().getId())
                .collect(Collectors.toList());

        // 4. 임베딩을 위한 텍스트 생성 (찜한 요리들의 제목과 재료를 조합)
        // 예: "김치찌개 돼지고기 김치 된장찌개 두부..." -> 사용자의 취향 텍스트
        StringBuilder queryBuilder = new StringBuilder();
        for (RecipeScrap scrap : scraps) {
            queryBuilder.append(scrap.getRecipe().getTitle()).append(" ");
            // 필요하다면 재료도 추가: queryBuilder.append(scrap.getRecipe().getIngredientsString()).append(" ");
        }
        String queryText = queryBuilder.toString().trim();

        // 5. 텍스트 임베딩 생성
        List<Float> queryVector = embeddingService.getEmbedding(queryText);

        // 6. Qdrant 필터 생성 (이미 찜한 레시피는 제외 - Must Not)
        Filter.Builder filterBuilder = Filter.newBuilder();
        for (Long excludedId : scrapedRecipeIds) {
            filterBuilder.addMustNot(Condition.newBuilder()
                    .setField(FieldCondition.newBuilder()
                            .setKey("recipe_id")
                            .setMatch(Match.newBuilder().setInteger(excludedId).build())
                            .build())
                    .build());
        }
        Filter filter = filterBuilder.build();

        try {
            // 7. Qdrant 유사도 검색
            List<Points.ScoredPoint> searchResult = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName("recipes")
                            .setFilter(filter) // 찜한거 제외 필터 적용
                            .addAllVector(queryVector)
                            .setLimit(5) // 5개 추천
                            .build()
            ).get();

            // 8. 결과 ID 추출
            List<Long> recommendedIds = searchResult.stream()
                    .map(point -> Long.parseLong(point.getId().getNum() + ""))
                    .collect(Collectors.toList());

            if (recommendedIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 9. MySQL 상세 조회
            List<Recipe> recipes = recipeRepository.findAllById(recommendedIds);

            // 10. Converter를 사용해 DTO 변환
            return recipes.stream()
                    .map(RecipeConverter::toRecipeDTO)
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Scrap based Qdrant Search Failed: memberId={}", memberId, e);
            throw new recipeException(ErrorStatus._RECIPE_SEARCH_FAIL);
        }
    }

    @Override
    public RecipeResponseDTO.RecipeInfoDTO getRecipe(Long recipeId) {
        // 1. 레시피 조회 (없으면 예외 발생)
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new recipeException(ErrorStatus._RECIPE_NOT_FOUND));

        // 2. 조회수 증가 로직 (선택 사항: 상세 조회 시 조회수 +1)
        recipe.increaseViewCount();

        // 3. Converter를 통해 상세 DTO 변환 및 반환
        return RecipeConverter.toRecipeInfoDTO(recipe);
    }

    @Override
    public List<RecipeResponseDTO.RecipeDTO> searchRecipe(RecipeRequestDTO.SearchRecipeDTO request) {

        Member member = memberRepository.findByKakaoId(request.getKakaoId())
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        // 1. 검색어가 없으면 빈 리스트 반환 (또는 전체 조회)
        if (request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 검색어 임베딩 생성 ("매운 국물 요리" -> 벡터 변환)
        List<Float> queryVector = embeddingService.getEmbedding(request.getKeyword());

        // 3. 필터 생성 (알레르기 필터)
        Filter.Builder filterBuilder = Filter.newBuilder();

        if (Boolean.TRUE.equals(request.getExcludeAllergy()) && member.getId() != null) {
            List<MemberPreference> preferences = memberPreferenceRepository.findAllByMemberId(member.getId());
            for (MemberPreference pref : preferences) {
                // 알레르기 재료가 포함된 레시피 제외 (Must Not)
                filterBuilder.addMustNot(Condition.newBuilder()
                        .setField(FieldCondition.newBuilder()
                                .setKey("ingredients") // Qdrant Payload Key
                                .setMatch(Match.newBuilder().setKeyword(pref.getIngredientName()).build())
                                .build())
                        .build());
            }
        }
        Filter filter = filterBuilder.build();

        try {
            // 4. Qdrant 검색 (관련도 점수 높은 순으로 자동 정렬됨)
            List<Points.ScoredPoint> searchResult = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName("recipes")
                            .setFilter(filter)
                            .addAllVector(queryVector)
                            .setLimit(20) // 상위 20개
                            .build()
            ).get();

            List<Long> recipeIds = searchResult.stream()
                    .map(point -> Long.parseLong(point.getId().getNum() + ""))
                    .collect(Collectors.toList());

            if (recipeIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 5. MySQL 조회 (순서 유지를 위해 로직 필요하거나, 간단히 조회)
            List<Recipe> recipes = recipeRepository.findAllById(recipeIds);

            // Qdrant가 준 순서(관련도 순)대로 recipes 리스트를 다시 정렬해야 함
            Map<Long, Recipe> recipeMap = recipes.stream()
                    .collect(Collectors.toMap(Recipe::getId, Function.identity()));

            return recipeIds.stream()
                    .filter(recipeMap::containsKey)
                    .map(id -> RecipeConverter.toRecipeDTO(recipeMap.get(id)))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Search failed", e);
            throw new recipeException(ErrorStatus._RECIPE_SEARCH_FAIL);
        }
    }

    @Override
    @Transactional
    public RecipeResponseDTO.RecipeScrapDTO scrapRecipe(Long recipeId, String kakaoId) {

        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new recipeException(ErrorStatus._RECIPE_NOT_FOUND));

        // 중복 체크
        if (recipeScrapRepository.existsByMemberAndRecipe(member, recipe)) {
            throw new recipeException(ErrorStatus._SCRAP_ALREADY_EXISTS);
        }

        // Converter를 사용해 Entity 생성
        RecipeScrap scrap = RecipeScrapConverter.toRecipeScrap(member, recipe);

        // 저장
        recipeScrapRepository.save(scrap);

        // 스크랩 횟수 증가
        recipe.increaseScrapsCount();

        // Converter를 사용해 DTO 변환 및 반환
        return RecipeScrapConverter.toRecipeScrapDTO(scrap);
    }

    @Override
    @Transactional
    public void deleteScrapRecipe(Long recipeId, String kakaoId) {

        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new memberException(ErrorStatus._MEMBER_NOT_FOUND));

        RecipeScrap scrap = recipeScrapRepository.findByMemberIdAndRecipeId(member.getId(), recipeId)
                .orElseThrow(() -> new recipeException(ErrorStatus._SCRAP_NOT_FOUND));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new recipeException(ErrorStatus._RECIPE_NOT_FOUND));

        recipe.decreaseScrapsCount();

        recipeScrapRepository.delete(scrap);
    }
}
