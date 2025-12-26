package capstone.fridge.domain.fridge.api;

import capstone.fridge.domain.fridge.api.dto.AutoPlaceDtos;
import capstone.fridge.domain.fridge.api.dto.FridgeBatchDtos;
import capstone.fridge.domain.fridge.api.dto.FridgeDtos;
import capstone.fridge.domain.fridge.service.FridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fridge")
public class FridgeController {

    private final FridgeService fridgeService;

    @PatchMapping
    public ResponseEntity<FridgeDtos.ListRes> list(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(fridgeService.list(memberId));
    }

    @PostMapping
    public ResponseEntity<FridgeDtos.IngredientRes> addManual(
            @AuthenticationPrincipal Long memberId,
            @RequestBody FridgeDtos.AddIngredientReq req
    ) {
        return ResponseEntity.ok(fridgeService.addManual(memberId, req));
    }

    @PostMapping("/ocr")
    public ResponseEntity<FridgeDtos.OcrRes> ocr(
            @AuthenticationPrincipal Long memberId,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(fridgeService.ocrAndSave(memberId, image));
    }

    @PostMapping("/auto-place")
    public ResponseEntity<FridgeDtos.AutoPlaceRes> autoPlace(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(fridgeService.autoPlace(memberId));
    }

    @DeleteMapping("/{ingredientId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long ingredientId
    ) {
        fridgeService.delete(memberId, ingredientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auto-place-llm")
    public ResponseEntity<AutoPlaceDtos.Res> autoPlaceLlm(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(fridgeService.autoPlaceByLlm(memberId));
    }

    @PostMapping(value = "/ocr-auto-place", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FridgeDtos.OcrAutoPlaceRes> ocrAutoPlace(
            @AuthenticationPrincipal Long memberId,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(fridgeService.ocrAutoPlace(memberId, image));
    }

    @PostMapping("/batch")
    public ResponseEntity<FridgeBatchDtos.BatchAddRes> addManualBatch(
            @AuthenticationPrincipal Long memberId,
            @RequestBody FridgeBatchDtos.BatchAddReq req
    ) {
        return ResponseEntity.ok(fridgeService.addManualBatch(memberId, req));
    }
}
