package capstone.fridge.domain.member.dto;

import lombok.Getter;

import java.util.List;

public class MemberRequestDTO {

    @Getter
    public static class UserPreferencesDTO {
        private List<String> allergies; // 알레르기 재료명 리스트
        private List<String> dislikes;  // 기피 음식 재료명 리스트
        private String age;
        private String gender;
    }
}
