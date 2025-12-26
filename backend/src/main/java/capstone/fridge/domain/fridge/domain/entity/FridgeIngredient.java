package capstone.fridge.domain.fridge.domain.entity;

import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.model.entity.BaseTimeEntity;
import capstone.fridge.domain.model.enums.InputMethod;
import jakarta.persistence.*;
import lombok.*;
import capstone.fridge.domain.fridge.domain.enums.FridgeSlot;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fridge_ingredient")
public class FridgeIngredient extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String name; // 재료명 (예: 두부)

    private String quantity; // 수량 (예: 1모, 300g)

    private LocalDate expiryDate; // 유통기한

    private String category;

    private String storageCategory; // 카테고리 (육류, 채소 등)

    // 실제 배치된 칸(룰 출력값)
    @Enumerated(EnumType.STRING)
    @Column(nullable = true) // 처음엔 null 가능(자동배치 전)
    private FridgeSlot fridgeSlot;

    @Enumerated(EnumType.STRING)
    private InputMethod inputMethod; // MANUAL, OCR

    private Long ingredientDataId;

    public void linkData(Long dataId) {
        this.ingredientDataId = dataId;
    }

    @Builder

    public FridgeIngredient(
            Member member,
            String name,
            String quantity,
            LocalDate expiryDate,
            String storageCategory,
            FridgeSlot fridgeSlot,
            InputMethod inputMethod
    ) {

    
        this.member = member;
        this.name = name;
        this.quantity = quantity;
        this.expiryDate = expiryDate;

        this.storageCategory = storageCategory;
        this.fridgeSlot = fridgeSlot;

        this.inputMethod = inputMethod;
    }

    public void assignSlot(FridgeSlot slot) {
        this.fridgeSlot = slot;
    }
}