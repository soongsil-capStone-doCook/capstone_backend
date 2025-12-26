package capstone.fridge.global.client.dto;

import java.util.List;

public class FastApiOcrDtos {

    public record OcrRes(
            String rawText,
            List<OcrItem> items
    ) {}

    public record OcrItem(
            String rawName,
            String name,
            String quantity,
            String expiryDate,
            String storageCategory
    ) {}
}
