package capstone.fridge.global.client.dto;

import java.util.List;

public class FastApiPlaceDtos {

    public record PlaceReq(List<Item> items) {}

    public record Item(
            Long id,
            String name,
            String quantity,
            String storageCategory
    ) {}

    public record PlaceRes(
            List<Placement> placements,
            List<Unplaced> unplaced
    ) {}

    public record Placement(
            Long id,
            String name,
            String storageCategory,
            String fridgeSlot
    ) {}

    public record Unplaced(
            Long id,
            String name,
            String reason
    ) {}
}
