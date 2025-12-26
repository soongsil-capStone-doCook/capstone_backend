package capstone.fridge.domain.fridge.api.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

public class AutoPlaceDtos {

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Res {
        @Builder.Default
        private List<PlacedItem> placed = new ArrayList<>();
        @Builder.Default
        private List<UnplacedItem> unplaced = new ArrayList<>();
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlacedItem {
        private Long id;
        private String name;
        private String storageCategory;
        private String fridgeSlot; // enum string
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnplacedItem {
        private Long id;
        private String name;
        private String reason;
    }
}
