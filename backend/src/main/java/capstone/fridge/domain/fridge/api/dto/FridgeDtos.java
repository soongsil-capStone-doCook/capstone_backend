package capstone.fridge.domain.fridge.api.dto;

import capstone.fridge.domain.fridge.domain.entity.FridgeIngredient;
import capstone.fridge.domain.fridge.domain.enums.FridgeSlot;
import capstone.fridge.domain.model.enums.InputMethod;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class FridgeDtos {

    @Getter @Setter
    public static class AddIngredientReq {
        private String name;
        private String quantity;
        private LocalDate expiryDate;
        private String storageCategory; // 분류(육류/채소/유제품/소스...)
    }


    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class IngredientRes {
        private Long id;
        private String name;
        private String quantity;
        private String storageCategory;
        private String fridgeSlot;
        private String inputMethod;

        public static IngredientRes from(FridgeIngredient e) {
            return new IngredientRes(
                    e.getId(),
                    e.getName(),
                    e.getQuantity(),
                    e.getStorageCategory(),
                    e.getFridgeSlot() == null ? null : e.getFridgeSlot().name(),
                    e.getInputMethod() == null ? null : e.getInputMethod().name()
            );
        }
    }

    @Getter @Builder
    public static class ListRes {
        private List<IngredientRes> items;
    }

    @Getter @Builder
    public static class OcrRes {
        private List<IngredientRes> saved;
    }

    @Getter @Builder
    public static class AutoPlaceRes {
        private List<IngredientRes> placed;
        private List<Long> unplacedIds;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OcrAutoPlaceRes {
        private String rawText;
        private List<IngredientRes> saved;
        private List<PlacedItem> placed;
        private List<UnplacedItem> unplaced;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PlacedItem {
        private Long id;
        private String name;
        private String storageCategory;
        private String fridgeSlot;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UnplacedItem {
        private Long id;
        private String name;
        private String reason;
    }


}
