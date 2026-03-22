package capstone.fridge.domain.notification.scheduler;

import capstone.fridge.domain.fridge.domain.entity.FridgeIngredient;
import capstone.fridge.domain.fridge.domain.repository.FridgeIngredientRepository;
import capstone.fridge.domain.member.domain.entity.Member;
//import capstone.fridge.domain.notification.service.NotificationService;
import capstone.fridge.domain.recipe.application.RecipeService;
import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final FridgeIngredientRepository fridgeIngredientRepository;
    //private final NotificationService notificationService;
    private final RecipeService recipeService; // 우리가 만든 하이브리드 로직 사용

    // 매일 오전 9시에 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendExpirationAlerts() {
        log.info("⏰ 유통기한 임박 재료 스캔 및 알림 시작...");

        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3); // 3일 남은 재료까지 조회

        // 1. 유통기한이 오늘 ~ 3일 뒤인 재료 모두 조회
        List<FridgeIngredient> expiringIngredients =
                fridgeIngredientRepository.findAllByExpiryDateBetween(today, threeDaysLater);

        if (expiringIngredients.isEmpty()) {
            log.info("임박한 재료가 없습니다.");
            return;
        }

        // 2. 사용자별로 재료 그룹화 (한 사람에게 알림 1통만 보내기 위해)
        Map<Member, List<FridgeIngredient>> memberIngredientsMap = expiringIngredients.stream()
                .collect(Collectors.groupingBy(FridgeIngredient::getMember));

        // 3. 각 사용자에게 맞춤 알림 생성 및 발송
        for (Map.Entry<Member, List<FridgeIngredient>> entry : memberIngredientsMap.entrySet()) {
            Member member = entry.getKey();
            List<FridgeIngredient> ingredients = entry.getValue();

            sendNotificationToMember(member, ingredients);
        }

        log.info("✅ 알림 발송 완료.");
    }

    private void sendNotificationToMember(Member member, List<FridgeIngredient> ingredients) {
        try {
            // 가장 급한(유통기한 짧은) 재료 이름 1개 추출
            String targetIngredientName = ingredients.get(0).getName();
            int count = ingredients.size();

            // 메시지 제목 구성
            String title = "🚨 냉장고 재료 심폐소생술이 필요해요!";

            // 메시지 본문 구성 (기본)
            String body = String.format("'%s' 포함 %d개의 재료 유통기한이 임박했습니다.", targetIngredientName, count);

            // ★ [핵심] 하이브리드 추천 로직을 호출하여 추천 요리 가져오기
            // 이미 우리가 유통기한 가중치를 넣었으므로, 1순위는 '임박 재료를 쓰는 요리'일 확률이 높음!
            List<RecipeResponseDTO.RecipeDTO> recommendations = recipeService.recommendRecipesHybrid(member.getId());

            if (!recommendations.isEmpty()) {
                // 추천된 1순위 요리 이름을 메시지에 포함
                String bestRecipeName = recommendations.get(0).getTitle();
                body = String.format("'%s'의 소비기한이 얼마 남지 않았어요! 오늘 저녁은 '%s' 어떠세요?", targetIngredientName, bestRecipeName);
            }

            // 알림 발송
            //notificationService.sendPush(member.getId(), title, body);

        } catch (Exception e) {
            log.error("알림 발송 중 오류 발생: MemberId={}", member.getId(), e);
        }
    }
}