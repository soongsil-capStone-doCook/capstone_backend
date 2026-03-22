package capstone.fridge.domain.recipe.application;

import capstone.fridge.domain.fridge.domain.entity.FridgeIngredient;
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
import capstone.fridge.domain.recipe.dto.HybridEmbeddingResponse;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

    private static final double WEIGHT_VECTOR = 0.8;
    private static final double WEIGHT_PREF = 0.2;

    @Override
    public List<RecipeResponseDTO.RecipeDTO> recommendRecipes(Long memberId) {

        // 1. 멤버 검증
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new memberException(ErrorStatus._BAD_REQUEST));

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

        Set<Long> scrapIds = getScrappedRecipeIds(memberId);

        // 5. Converter를 사용하여 DTO 변환 및 반환
        return cookableRecipes.stream()
                .map(recipe -> RecipeConverter.toRecipeDTO(recipe, scrapIds.contains(recipe.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeResponseDTO.RecipeDTO> recommendMissingRecipes(Long memberId) {

        // 1. 멤버 검증
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new memberException(ErrorStatus._BAD_REQUEST));

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

            Set<Long> scrapIds = getScrappedRecipeIds(memberId);

            return recipes.stream()
                    .map(recipe -> {
                        List<String> recipeIngredientNames = recipe.getIngredients().stream()
                                .map(RecipeIngredient::getName)
                                .collect(Collectors.toList());

                        List<String> missingIngredients = new ArrayList<>(recipeIngredientNames);
                        missingIngredients.removeAll(ingredients);

                        return RecipeConverter.toRecipeDTO(recipe, missingIngredients, scrapIds.contains(recipe.getId()));
                    })
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant Search Failed: memberId={}", memberId, e);
            throw new recipeException(ErrorStatus._RECIPE_SEARCH_FAIL);
        }
    }

    @Override
    public List<RecipeResponseDTO.RecipeDTO> recommendScrapsRecipes(Long memberId) {

        // 1. 사용자 검증
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new memberException(ErrorStatus._BAD_REQUEST));

        // 2. 사용자가 찜한(Scrap) 레시피 목록 조회
        List<RecipeScrap> scraps = recipeScrapRepository.findAllByMemberIdWithRecipe(memberId);

        Set<Long> scrapIds = getScrappedRecipeIds(memberId);

        if (scraps.isEmpty()) {
            log.info("찜한 레시피가 없어 랜덤 추천을 실행합니다. memberId={}", memberId);

            // DB에서 랜덤하게 5개 가져오기 (Native Query 사용 시 가장 효율적)
            List<Recipe> randomRecipes = recipeRepository.findRandomRecipes(5);

            return randomRecipes.stream()
                    .map(recipe -> RecipeConverter.toRecipeDTO(recipe, scrapIds.contains(recipe.getId())))
                    .collect(Collectors.toList());
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
                    .map(recipe -> RecipeConverter.toRecipeDTO(recipe, scrapIds.contains(recipe.getId())))
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Scrap based Qdrant Search Failed: memberId={}", memberId, e);
            throw new recipeException(ErrorStatus._RECIPE_SEARCH_FAIL);
        }
    }

    @Override
    @Transactional
    public RecipeResponseDTO.RecipeInfoDTO getRecipe(Long recipeId, Long memberId) {
        // 1. 레시피 조회
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new recipeException(ErrorStatus._RECIPE_NOT_FOUND));

        // 2. 조회수 증가
        recipe.increaseViewCount(); // 메서드명 확인

        // 3. 찜 여부 확인 로직
        boolean isScrapped = false;

        // 로그인한 사용자(memberId가 있는 경우)만 DB 조회
        if (memberId != null) {
            isScrapped = recipeScrapRepository.existsByMemberIdAndRecipeId(memberId, recipeId);
        }

        // 4. Converter 호출 (isScrapped 전달)
        return RecipeConverter.toRecipeInfoDTO(recipe, isScrapped);
    }

    @Override
    public List<RecipeResponseDTO.RecipeDTO> searchRecipe(Long memberId, RecipeRequestDTO.SearchRecipeDTO request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new memberException(ErrorStatus._BAD_REQUEST));

        // 1. 검색어가 없으면 빈 리스트 반환 (또는 전체 조회)
        if (request == null || request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 검색어 임베딩 생성 ("매운 국물 요리" -> 벡터 변환)
        List<Float> queryVector = embeddingService.getEmbedding(request.getKeyword());

        // 3. 기피 재료 목록 추출 및 Qdrant 필터 생성
        // ★ 수정된 부분: 조건에 따라 한 번만 초기화되도록 변경 (effectively final 만족)
        final List<String> excludedIngredients;
        Filter.Builder filterBuilder = Filter.newBuilder();

        if (Boolean.TRUE.equals(request.getExcludeAllergy())) {
            List<MemberPreference> preferences = memberPreferenceRepository.findAllByMemberId(member.getId());

            excludedIngredients = preferences.stream()
                    .map(MemberPreference::getIngredientName)
                    .collect(Collectors.toList());

            for (String excluded : excludedIngredients) {
                filterBuilder.addMustNot(Condition.newBuilder()
                        .setField(FieldCondition.newBuilder()
                                .setKey("ingredients")
                                .setMatch(Match.newBuilder().setKeyword(excluded).build())
                                .build())
                        .build());
            }
        } else {
            // 알레르기 제외 옵션이 꺼져있으면 빈 리스트 할당
            excludedIngredients = Collections.emptyList();
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
                    .map(point -> point.getId().getNum())
                    .collect(Collectors.toList());

            if (recipeIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 5. MySQL 조회 (순서 유지를 위해 로직 필요하거나, 간단히 조회)
            List<Recipe> recipes = recipeRepository.findAllById(recipeIds);

            // Qdrant가 준 순서(관련도 순)대로 recipes 리스트를 다시 정렬해야 함
            Map<Long, Recipe> recipeMap = recipes.stream()
                    .collect(Collectors.toMap(Recipe::getId, Function.identity()));

            Set<Long> scrapIds = getScrappedRecipeIds(memberId);
            List<String> userIngredients = fridgeIngredientRepository.findIngredientNamesByMemberId(member.getId());

            // 6. [추가] 이중 필터링 및 DTO 변환
            return recipeIds.stream()
                    .filter(recipeMap::containsKey)
                    .map(recipeMap::get) // ID로 Recipe 객체 추출
                    .filter(recipe -> isSafeRecipe(recipe, excludedIngredients))
                    .map(recipe -> {
                        List<String> recipeIngredientNames = recipe.getIngredients().stream()
                                .map(RecipeIngredient::getName)
                                .collect(Collectors.toList());

                        List<String> missingIngredients = new ArrayList<>(recipeIngredientNames);
                        missingIngredients.removeAll(userIngredients);

                        return RecipeConverter.toRecipeDTO(recipe, missingIngredients, scrapIds.contains(recipe.getId()));
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Search failed", e);
            throw new recipeException(ErrorStatus._RECIPE_SEARCH_FAIL);
        }
    }

    @Override
    @Transactional
    public RecipeResponseDTO.RecipeScrapDTO scrapRecipe(Long recipeId, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new memberException(ErrorStatus._BAD_REQUEST));

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
    public void deleteScrapRecipe(Long recipeId, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new memberException(ErrorStatus._BAD_REQUEST));

        RecipeScrap scrap = recipeScrapRepository.findByMemberIdAndRecipeId(member.getId(), recipeId)
                .orElseThrow(() -> new recipeException(ErrorStatus._SCRAP_NOT_FOUND));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new recipeException(ErrorStatus._RECIPE_NOT_FOUND));

        recipe.decreaseScrapsCount();

        recipeScrapRepository.delete(scrap);
    }

    // 1. [냉장고 재료 맞춤 추천] 냉장고 재료를 '키워드'로 사용하여 하이브리드 벡터 검색을 수행하고 MySQL과 결합합니다.
    @Override
    public List<RecipeResponseDTO.RecipeDTO> recommendRecipesHybrid(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new memberException(ErrorStatus._BAD_REQUEST));

        List<FridgeIngredient> myIngredients = fridgeIngredientRepository.findAllByMemberId(memberId);
        if (myIngredients.isEmpty()) return Collections.emptyList();

        List<String> ingredientNames = myIngredients.stream().map(FridgeIngredient::getName).collect(Collectors.toList());
        String queryText = String.join(" ", ingredientNames);

        // 하이브리드 검색 수행 (내부적으로 2번과 동일한 로직 사용 가능)
        return getHybridSearchResults(memberId, queryText, ingredientNames, true);
    }


    // 2. [부족한 재료 포함 추천]
    @Override
    public List<RecipeResponseDTO.RecipeDTO> recommendMissingRecipesHybrid(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new memberException(ErrorStatus._BAD_REQUEST));

        List<FridgeIngredient> myIngredients = fridgeIngredientRepository.findAllByMemberId(memberId);
        if (myIngredients.isEmpty()) return Collections.emptyList();

        List<String> ingredientNames = myIngredients.stream().map(FridgeIngredient::getName).collect(Collectors.toList());
        String queryText = String.join(" ", ingredientNames);

        return getHybridSearchResults(memberId, queryText, ingredientNames, false);
    }


     //3. [검색어 기반 하이브리드 검색]
    @Override
    public List<RecipeResponseDTO.RecipeDTO> searchRecipeHybrid(Long memberId, RecipeRequestDTO.SearchRecipeDTO request) {
        return searchRecipeHybrid(memberId, request, 30);
    }

    @Override
    public List<RecipeResponseDTO.RecipeDTO> searchRecipeHybrid(Long memberId, RecipeRequestDTO.SearchRecipeDTO request, int topK) {
        if (request == null || request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> userIngredients = memberId != null
                ? fridgeIngredientRepository.findIngredientNamesByMemberId(memberId)
                : List.of();
        return getHybridSearchResults(memberId, request.getKeyword(), userIngredients, request.getExcludeAllergy(), topK);
    }

    @Override
    public List<RecipeResponseDTO.RecipeDTO> searchRecipeDenseOnly(Long memberId, String queryText, int topK) {
        return getDenseOnlySearchResults(memberId, queryText, topK);
    }


    // [공통 하이브리드 검색 엔진 로직] — 단계별 로그로 어디서 실패하는지 확인
    private List<RecipeResponseDTO.RecipeDTO> getHybridSearchResults(Long memberId, String queryText, List<String> userIngredients, Boolean excludeAllergy) {
        return getHybridSearchResults(memberId, queryText, userIngredients, excludeAllergy, 30);
    }

    private List<RecipeResponseDTO.RecipeDTO> getHybridSearchResults(Long memberId, String queryText, List<String> userIngredients, Boolean excludeAllergy, int topK) {
        String step = "";
        try {
            step = "1.getHybridEmbedding";
            log.debug("[HybridSearch] step={}", step);
            HybridEmbeddingResponse emb = embeddingService.getHybridEmbedding(queryText);
            int sparseSize = (emb.getSparse() == null) ? 0 : (emb.getSparse().getIndices() == null ? 0 : emb.getSparse().getIndices().size());
            log.warn("[HybridSearch] 임베딩 응답 sparse 개수={} (0이면 임베딩서버가 sparse 안 보냄)", sparseSize);

            step = "2.getExcludedIngredients_and_filter";
            log.debug("[HybridSearch] step={}", step);
            List<String> excludedIngredients = getExcludedIngredients(memberId);
            // Qdrant에 "ingredients" payload keyword 인덱스가 없으면 필터 사용 시 INVALID_ARGUMENT → 빈 필터로 검색 후 Java에서 isSafeRecipe로 기피 재료 제외
            Points.Filter filter = Points.Filter.newBuilder().build();

            step = "3.build_denseSearch";
            log.debug("[HybridSearch] step={}", step);
            Points.SearchPoints denseSearch = Points.SearchPoints.newBuilder()
                    .setCollectionName("recipes")
                    .addAllVector(emb.getDense())
                    .setVectorName("dense")
                    .setFilter(filter)
                    .setLimit(50)
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();

            step = "4.build_sparseSearch";
            log.debug("[HybridSearch] step={}", step);
            Points.SparseIndices sparseIndices = Points.SparseIndices.newBuilder()
                    .addAllData(emb.getSparse().getIndices())
                    .build();

            Points.SearchPoints sparseSearch = Points.SearchPoints.newBuilder()
                    .setCollectionName("recipes")
                    .setVectorName("sparse")
                    .addAllVector(emb.getSparse().getValues())
                    .setSparseIndices(sparseIndices)
                    .setFilter(filter)
                    .setLimit(50)
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();

            step = "5.qdrant_dense_search";
            log.debug("[HybridSearch] step={}", step);
            List<Points.ScoredPoint> denseResults = qdrantClient.searchAsync(denseSearch).get();

            step = "6.qdrant_sparse_search";
            log.debug("[HybridSearch] step={}", step);
            List<Points.ScoredPoint> sparseResults = qdrantClient.searchAsync(sparseSearch).get();
            log.warn("[HybridSearch] denseResults={} sparseResults={} (0이면 sparse 미동작)", denseResults.size(), sparseResults.size());

            step = "7.mergeRRF_and_mysql";
            log.debug("[HybridSearch] step={}", step);
            Map<Long, Double> vectorScoreMap = mergeRRF(denseResults, sparseResults);
            List<Recipe> candidates = recipeRepository.findAllById(vectorScoreMap.keySet());
            Set<Long> scrapIds = getScrappedRecipeIds(memberId);

            step = "8.calculateHybridScoresAndSort_and_convert";
            log.debug("[HybridSearch] step={}", step);
            List<Recipe> sortedRecipes = calculateHybridScoresAndSort(candidates, scrapIds, vectorScoreMap);

            return sortedRecipes.stream()
                    .filter(recipe -> isSafeRecipe(recipe, excludedIngredients))
                    .limit(topK)
                    .map(recipe -> {
                        List<String> recipeIngredientNames = recipe.getIngredients().stream()
                                .map(RecipeIngredient::getName).collect(Collectors.toList());
                        List<String> missing = new ArrayList<>(recipeIngredientNames);
                        missing.removeAll(userIngredients);
                        return RecipeConverter.toRecipeDTO(recipe, missing, scrapIds.contains(recipe.getId()));
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Hybrid Search Failed at step [{}] | exception={} | message={}", step, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new recipeException(ErrorStatus._RECIPE_SEARCH_FAIL);
        }
    }

    /** 실험용: Dense 벡터만 사용한 검색 (Ablation Baseline) */
    private List<RecipeResponseDTO.RecipeDTO> getDenseOnlySearchResults(Long memberId, String queryText, int topK) {
        try {
            HybridEmbeddingResponse emb = embeddingService.getHybridEmbedding(queryText);
            Points.Filter filter = Points.Filter.newBuilder().build();
            Points.SearchPoints denseSearch = Points.SearchPoints.newBuilder()
                    .setCollectionName("recipes")
                    .addAllVector(emb.getDense())
                    .setVectorName("dense")
                    .setFilter(filter)
                    .setLimit(Math.max(topK, 50))
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();

            List<Points.ScoredPoint> denseResults = qdrantClient.searchAsync(denseSearch).get();
            List<Long> orderedIds = denseResults.stream()
                    .map(p -> p.getId().getNum())
                    .limit(topK)
                    .toList();
            if (orderedIds.isEmpty()) return List.of();

            List<Recipe> candidates = recipeRepository.findAllById(orderedIds);
            Map<Long, Integer> rankMap = new HashMap<>();
            for (int i = 0; i < orderedIds.size(); i++) rankMap.put(orderedIds.get(i), i);
            List<Recipe> sortedRecipes = candidates.stream()
                    .sorted(Comparator.comparingInt(r -> rankMap.getOrDefault(r.getId(), Integer.MAX_VALUE)))
                    .toList();

            List<String> excludedIngredients = getExcludedIngredients(memberId);
            Set<Long> scrapIds = getScrappedRecipeIds(memberId);
            List<String> userIngredients = memberId != null
                    ? fridgeIngredientRepository.findIngredientNamesByMemberId(memberId)
                    : List.of();

            return sortedRecipes.stream()
                    .filter(recipe -> isSafeRecipe(recipe, excludedIngredients))
                    .limit(topK)
                    .map(recipe -> {
                        List<String> recipeIngredientNames = recipe.getIngredients().stream()
                                .map(RecipeIngredient::getName).collect(Collectors.toList());
                        List<String> missing = new ArrayList<>(recipeIngredientNames);
                        missing.removeAll(userIngredients);
                        return RecipeConverter.toRecipeDTO(recipe, missing, scrapIds.contains(recipe.getId()));
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Dense-only search failed", e);
            throw new recipeException(ErrorStatus._RECIPE_SEARCH_FAIL);
        }
    }

    private Set<Long> getScrappedRecipeIds(Long memberId) {
        if (memberId == null) return new HashSet<>();
        return new HashSet<>(recipeScrapRepository.findRecipeIdsByMemberId(memberId));
    }

    @Getter
    @AllArgsConstructor
    private static class ScoredRecipe {
        Recipe recipe;
        double finalScore;
    }

    private List<Recipe> calculateHybridScoresAndSort(List<Recipe> recipes,
                                                      Set<Long> scrapIds,
                                                      Map<Long, Double> vectorScoreMap) {
        final double WEIGHT_VECTOR = 0.8;
        final double WEIGHT_PREF = 0.2;

        return recipes.stream()
                .map(recipe -> {
                    double vectorScore = vectorScoreMap.getOrDefault(recipe.getId(), 0.0) * 100;
                    double prefScore = scrapIds.contains(recipe.getId()) ? 1.0 : 0.0;
                    double finalScore = (vectorScore * WEIGHT_VECTOR) + (prefScore * WEIGHT_PREF);

                    return new ScoredRecipe(recipe, finalScore);
                })
                .sorted((o1, o2) -> Double.compare(o2.getFinalScore(), o1.getFinalScore()))
                .map(ScoredRecipe::getRecipe)
                .collect(Collectors.toList());
    }

    // 2. 유통기한 임박 점수 계산 로직
    private double calculateUrgencyScore(Recipe recipe, Map<String, LocalDate> expirationMap) {
        long minDaysLeft = 30; // 기본값
        boolean hasMatch = false;

        for (RecipeIngredient ri : recipe.getIngredients()) {
            if (expirationMap.containsKey(ri.getName())) {
                hasMatch = true;
                long days = ChronoUnit.DAYS.between(LocalDate.now(), expirationMap.get(ri.getName()));
                if (days < minDaysLeft) {
                    minDaysLeft = days; // 가장 급한 재료 기준
                }
            }
        }

        if (!hasMatch) return 0.0;

        // 점수 부여 정책
        if (minDaysLeft <= 3) return 1.0;  // 3일 이내: 100점
        if (minDaysLeft <= 7) return 0.5;  // 7일 이내: 50점
        return 0.1;                        // 그 외: 10점
    }

    // 3. 안전 필터 (이중 체크)
    private boolean isSafeRecipe(Recipe recipe, List<String> excludedIngredients) {
        if (excludedIngredients == null || excludedIngredients.isEmpty()) return true;

        // 레시피 재료 중 하나라도 기피 목록에 포함되면 false
        return recipe.getIngredients().stream()
                .noneMatch(ri -> excludedIngredients.contains(ri.getName()));
    }

    // 4. 기피 재료 목록 조회 헬퍼
    private List<String> getExcludedIngredients(Long memberId) {
        List<MemberPreference> preferences = memberPreferenceRepository.findAllByMemberId(memberId);
        List<String> excluded = preferences.stream()
                .map(MemberPreference::getIngredientName)
                .collect(Collectors.toList());
        if (excluded.isEmpty()) excluded.add(""); // IN절 에러 방지용 더미
        return excluded;
    }

    // 5. Qdrant 제외 필터 생성 헬퍼
    private Filter createExclusionFilter(List<String> excludedIngredients) {
        Filter.Builder filterBuilder = Filter.newBuilder();
        for (String excluded : excludedIngredients) {
            if(excluded.isEmpty()) continue;
            filterBuilder.addMustNot(Condition.newBuilder()
                    .setField(FieldCondition.newBuilder()
                            .setKey("ingredients")
                            .setMatch(Match.newBuilder().setKeyword(excluded).build())
                            .build())
                    .build());
        }
        return filterBuilder.build();
    }

    private Map<Long, Double> mergeRRF(List<Points.ScoredPoint> dense, List<Points.ScoredPoint> sparse) {
        Map<Long, Double> rrfScores = new HashMap<>();
        int k = 60; // RRF 상수 (일반적으로 60 사용)

        for (int i = 0; i < dense.size(); i++) {
            long id = dense.get(i).getId().getNum();
            double score = 1.0 / (k + i + 1);
            rrfScores.put(id, rrfScores.getOrDefault(id, 0.0) + score);
        }
        for (int i = 0; i < sparse.size(); i++) {
            long id = sparse.get(i).getId().getNum();
            double score = 1.0 / (k + i + 1);
            rrfScores.put(id, rrfScores.getOrDefault(id, 0.0) + score);
        }
        return rrfScores;
    }
}
