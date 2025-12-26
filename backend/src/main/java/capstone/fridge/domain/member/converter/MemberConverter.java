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
        return MemberResponseDTO.UserInfoDTO.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .profileImageUrl(member.getProfileImageUrl())
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
                .dislikes(dislikes)
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
        if (request.getDislikes() != null) {
            request.getDislikes().forEach(name -> {
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
                    // ID 기반 썸네일 URL 생성 로직 적용
                    String thumbnailUrl = S3_BASE_URL + "/recipes/" + recipe.getId() + "/main.png";

                    return MemberResponseDTO.ScrapRecipeDTO.builder()
                            .recipeId(recipe.getId())
                            .title(recipe.getTitle())
                            .thumbnailUrl(thumbnailUrl) // 생성된 URL 주입
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
