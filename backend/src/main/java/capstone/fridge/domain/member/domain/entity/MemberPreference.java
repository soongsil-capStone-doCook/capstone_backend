package capstone.fridge.domain.member.domain.entity;

import capstone.fridge.domain.model.enums.PreferenceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_preference")
public class MemberPreference {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String ingredientName; // 예: "새우", "오이"

    @Enumerated(EnumType.STRING)
    private PreferenceType type; // ALLERGY(알레르기), DISLIKE(기피)

    @Builder
    public MemberPreference(Member member, String ingredientName, PreferenceType type) {
        this.member = member;
        this.ingredientName = ingredientName;
        this.type = type;
    }
}