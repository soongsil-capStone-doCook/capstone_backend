package capstone.fridge.domain.member.converter;

import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.member.domain.entity.MemberPreference;
import capstone.fridge.domain.member.dto.MemberRequestDTO;
import capstone.fridge.domain.member.dto.MemberResponseDTO;
import capstone.fridge.domain.model.enums.PreferenceType;
import capstone.fridge.domain.recipe.domain.entity.Recipe;
import capstone.fridge.domain.scrap.domain.entity.RecipeScrap;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MemberConverter {

    private static final String S3_BASE_URL = "https://capstone-fridge.s3.ap-northeast-2.amazonaws.com/recipes/"; // 실제 버킷 주소로 수정 필요

    public static MemberResponseDTO.UserInfoDTO toUserInfoDTO(Member member) {
        // 1. 알레르기 리스트 추출
        List<String> allergies = member.getPreferences().stream()
                .filter(p -> p.getType() == PreferenceType.ALLERGY)
                .map(MemberPreference::getIngredientName)
                .collect(Collectors.toList());

        // 2. 기피 음식 리스트 추출
        List<String> dislikes = member.getPreferences().stream()
                .filter(p -> p.getType() == PreferenceType.DISLIKE)
                .map(MemberPreference::getIngredientName)
                .collect(Collectors.toList());

        // 3. DTO 생성 및 반환
        return MemberResponseDTO.UserInfoDTO.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .profileImageUrl(member.getProfileImageUrl())
                .allergies(allergies) // 추가
                .dislikedIngredients(dislikes)   // 추가
                .build();
    }

    // List<MemberPreference> -> UserPreferencesDTO (Response)
    public static MemberResponseDTO.UserPreferencesDTO toUserPreferencesDTO(Member member, List<MemberPreference> preferences) {

        List<String> allergies = preferences.stream()
                .filter(p -> p.getType() == PreferenceType.ALLERGY)
                .map(MemberPreference::getIngredientName)
                .collect(Collectors.toList());

        List<String> dislikes = preferences.stream()
                .filter(p -> p.getType() == PreferenceType.DISLIKE)
                .map(MemberPreference::getIngredientName)
                .collect(Collectors.toList());

        return MemberResponseDTO.UserPreferencesDTO.builder()
                .allergies(allergies)
                .dislikedIngredients(dislikes)
                .age(member.getAge())
                .gender(member.getGender())
                .build();
    }

    // Request DTO -> List<MemberPreference> (Entity 생성)
    public static List<MemberPreference> toMemberPreferenceEntities(Member member, MemberRequestDTO.UserPreferencesDTO request) {
        List<MemberPreference> preferenceList = new ArrayList<>();

        // 알레르기 리스트 변환
        if (request.getAllergies() != null) {
            request.getAllergies().forEach(name -> {
                preferenceList.add(MemberPreference.builder()
                        .member(member)
                        .ingredientName(name)
                        .type(PreferenceType.ALLERGY)
                        .build());
            });
        }

        // 기피 음식 리스트 변환
        if (request.getDislikedIngredients() != null) {
            request.getDislikedIngredients().forEach(name -> {
                preferenceList.add(MemberPreference.builder()
                        .member(member)
                        .ingredientName(name)
                        .type(PreferenceType.DISLIKE)
                        .build());
            });
        }

        return preferenceList;
    }

    // List<RecipeScrap> -> UserScrapsDTO
    public static MemberResponseDTO.UserScrapsDTO toUserScrapsDTO(List<RecipeScrap> scrapList) {
        List<MemberResponseDTO.ScrapRecipeDTO> scrapDTOs = scrapList.stream()
                .map(scrap -> {
                    Recipe recipe = scrap.getRecipe();

                    // 추천 로직과 동일하게 URL 조립 (중복된 "/recipes/" 제거)
                    String mainImageUrl = S3_BASE_URL + recipe.getId() + "/main.png";

                    return MemberResponseDTO.ScrapRecipeDTO.builder()
                            .recipeId(recipe.getId())
                            .title(recipe.getTitle())
                            .thumbnailUrl(mainImageUrl) // 수정된 DTO 필드명에 맞춤
                            .build();
                })
                .collect(Collectors.toList());

        return MemberResponseDTO.UserScrapsDTO.builder()
                .scrapList(scrapDTOs)
                .build();
    }

    public static MemberResponseDTO.OnboardingStatusDTO toOnboardingStatusDTO(Member member, boolean isOnboarded) {
        return MemberResponseDTO.OnboardingStatusDTO.builder()
                .memberId(member.getId())
                .isOnboarded(isOnboarded)
                .build();
    }
}
