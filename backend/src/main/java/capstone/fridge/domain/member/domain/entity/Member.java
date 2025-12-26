package capstone.fridge.domain.member.domain.entity;

import capstone.fridge.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    // 카카오 고유 ID (예: 123456789)
    @Column(nullable = false, unique = true)
    private String kakaoId;

    @Column(nullable = false)
    private String nickname;

    private String email;

    private String profileImageUrl;

    private String age;

    private String gender;

    // 알레르기 및 기피 음식 (1:N)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberPreference> preferences = new ArrayList<>();

    @Builder
    public Member(String kakaoId, String nickname, String email, String profileImageUrl, String age, String gender) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.age = age;
        this.gender = gender;
    }

    public void updateProfile(String age, String gender) {
        if (age != null) {
            this.age = age;
        }
        if (gender != null) {
            this.gender = gender;
        }
    }
}