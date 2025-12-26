package capstone.fridge.domain.fridge.service;

import capstone.fridge.domain.fridge.api.dto.AutoPlaceDtos;
import capstone.fridge.domain.fridge.api.dto.FridgeBatchDtos;
import capstone.fridge.domain.fridge.api.dto.FridgeDtos;
import capstone.fridge.domain.fridge.domain.entity.FridgeIngredient;
import capstone.fridge.domain.fridge.domain.enums.FridgeSlot;
import capstone.fridge.domain.fridge.domain.repository.FridgeIngredientRepository;
import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.member.domain.repository.MemberRepository;
import capstone.fridge.domain.model.enums.InputMethod;
import capstone.fridge.global.client.FastApiClient;
import capstone.fridge.global.client.dto.FastApiOcrDtos;
import capstone.fridge.global.client.dto.FastApiPlaceDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FridgeService {

    private final MemberRepository memberRepository;
    private final FridgeIngredientRepository fridgeIngredientRepository;
    private final OcrService ocrService; // 기존 OCR 유지(원하면 제거 가능)
    private final FastApiClient fastApiClient;

    public FridgeDtos.IngredientRes addManual(Long memberId, FridgeDtos.AddIngredientReq req) {
        Member member = memberRepository.findById(memberId).orElseThrow();

        // 1) DB 저장(우선 fridgeSlot은 null)
        FridgeIngredient saved = fridgeIngredientRepository.save(
                FridgeIngredient.builder()
                        .member(member)
                        .name(req.getName())
                        .quantity(req.getQuantity())
                        .expiryDate(req.getExpiryDate())
                        .storageCategory(req.getStorageCategory())
                        .inputMethod(InputMethod.MANUAL)
                        .build()
        );

        // 2) FastAPI /place에 "1개 아이템"만 요청
        FastApiPlaceDtos.Item item = new FastApiPlaceDtos.Item(
                saved.getId(),
                saved.getName(),
                saved.getQuantity(),
                saved.getStorageCategory()
        );

        FastApiPlaceDtos.PlaceRes placeRes =
                fastApiClient.place(new FastApiPlaceDtos.PlaceReq(List.of(item)));

        // 3) placements에 내가 방금 저장한 id가 있으면 slot assign + 저장(더티체킹)
        if (placeRes != null && placeRes.placements() != null) {
            placeRes.placements().stream()
                    .filter(p -> p != null && p.id() != null && p.id().equals(saved.getId()))
                    .findFirst()
                    .ifPresent(p -> {
                        try {
                            FridgeSlot slot = FridgeSlot.valueOf(p.fridgeSlot());
                            saved.assignSlot(slot);
                        } catch (Exception ignored) {
                            // enum 값 이상하면 그냥 slot 미지정으로 둠
                        }
                    });
        }

        // 4) 최종 응답(프론트는 fridgeSlot 포함해서 바로 받음)
        return FridgeDtos.IngredientRes.from(saved);
    }

    public FridgeBatchDtos.BatchAddRes addManualBatch(Long memberId, FridgeBatchDtos.BatchAddReq req) {
        Member member = memberRepository.findById(memberId).orElseThrow();

        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            return new FridgeBatchDtos.BatchAddRes(List.of());
        }

        // 1) DB 저장(여러개)
        List<FridgeIngredient> saved = new ArrayList<>();
        for (FridgeDtos.AddIngredientReq r : req.getItems()) {
            if (r == null) continue;

            FridgeIngredient e = FridgeIngredient.builder()
                    .member(member)
                    .name(r.getName())
                    .quantity(r.getQuantity())
                    .expiryDate(r.getExpiryDate())
                    .storageCategory(r.getStorageCategory())
                    .inputMethod(InputMethod.MANUAL)
                    .build();

            saved.add(e);
        }
        saved = fridgeIngredientRepository.saveAll(saved);

        // 2) FastAPI /place 요청 1번(아이템 N개)
        List<FastApiPlaceDtos.Item> placeItems = saved.stream()
                .map(e -> new FastApiPlaceDtos.Item(
                        e.getId(),
                        e.getName(),
                        e.getQuantity(),
                        e.getStorageCategory()
                ))
                .toList();

        FastApiPlaceDtos.PlaceRes placeRes =
                fastApiClient.place(new FastApiPlaceDtos.PlaceReq(placeItems));

        // 3) placements 결과 반영 (id -> entity 매핑)
        Map<Long, FridgeIngredient> byId = saved.stream()
                .collect(Collectors.toMap(FridgeIngredient::getId, x -> x));

        if (placeRes != null && placeRes.placements() != null) {
            for (FastApiPlaceDtos.Placement p : placeRes.placements()) {
                if (p == null || p.id() == null) continue;

                FridgeIngredient target = byId.get(p.id());
                if (target == null) continue;

                try {
                    FridgeSlot slot = FridgeSlot.valueOf(p.fridgeSlot());
                    target.assignSlot(slot);
                } catch (Exception ignored) {
                    // 잘못된 enum이면 skip
                }
            }
        }

        // 4) 응답
        List<FridgeDtos.IngredientRes> out = saved.stream()
                .map(FridgeDtos.IngredientRes::from)
                .toList();

        return new FridgeBatchDtos.BatchAddRes(out);
    }

    @Transactional(readOnly = true)
    public FridgeDtos.ListRes list(Long memberId) {
        List<FridgeIngredient> items = fridgeIngredientRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);
        return FridgeDtos.ListRes.builder()
                .items(items.stream().map(this::toRes).toList())
                .build();
    }

    public void delete(Long memberId, Long ingredientId) {
        FridgeIngredient ingredient = fridgeIngredientRepository
                .findByIdAndMemberId(ingredientId, memberId)
                .orElseThrow();
        fridgeIngredientRepository.delete(ingredient);
    }

    // (기존) 구글 비전 OCR 등으로 저장
    public FridgeDtos.OcrRes ocrAndSave(Long memberId, MultipartFile image) {
        Member member = memberRepository.findById(memberId).orElseThrow();

        List<OcrService.OcrItem> ocrItems = ocrService.extract(image);

        List<FridgeIngredient> saved = ocrItems.stream().map(it ->
                FridgeIngredient.builder()
                        .member(member)
                        .name(it.name())
                        .quantity(it.quantity())
                        .storageCategory(it.storageCategory())
                        .inputMethod(InputMethod.OCR)
                        .build()
        ).map(fridgeIngredientRepository::save).toList();

        return FridgeDtos.OcrRes.builder()
                .saved(saved.stream().map(this::toRes).toList())
                .build();
    }

    // (기존) 룰 기반 오토플레이스
    public FridgeDtos.AutoPlaceRes autoPlace(Long memberId) {
        memberRepository.findById(memberId).orElseThrow();

        List<FridgeIngredient> targets =
                fridgeIngredientRepository.findAllByMemberIdAndFridgeSlotIsNullOrderByCreatedAtDesc(memberId);

        List<FridgeIngredient> placed = new ArrayList<>();
        List<Long> unplaced = new ArrayList<>();

        for (FridgeIngredient ing : targets) {
            FridgeSlot slot = ruleBasedSlot(ing.getStorageCategory(), ing.getName());
            if (slot == null) {
                unplaced.add(ing.getId());
                continue;
            }
            ing.assignSlot(slot);
            placed.add(ing);
        }

        return FridgeDtos.AutoPlaceRes.builder()
                .placed(placed.stream().map(this::toRes).toList())
                .unplacedIds(unplaced)
                .build();
    }

    private FridgeSlot ruleBasedSlot(String storageCategory, String name) {
        if (storageCategory == null) return null;

        return switch (storageCategory) {
            case "채소", "과일" -> FridgeSlot.CRISPER_DRAWER;
            case "육류", "해산물" -> FridgeSlot.MAIN_SHELF_3;
            case "유제품" -> FridgeSlot.MAIN_SHELF_2;
            case "소스", "양념", "음료" -> FridgeSlot.DOOR_SHELF_2;
            default -> null;
        };
    }

    private FridgeDtos.IngredientRes toRes(FridgeIngredient e) {
        return FridgeDtos.IngredientRes.builder()
                .id(e.getId())
                .name(e.getName())
                .quantity(e.getQuantity())
                .storageCategory(e.getStorageCategory())
                .fridgeSlot(e.getFridgeSlot() == null ? null : e.getFridgeSlot().name())
                .inputMethod(e.getInputMethod() == null ? null : e.getInputMethod().name())
                .build();
    }

    // ✅ LLM place만 호출해서 slot 저장
    public AutoPlaceDtos.Res autoPlaceByLlm(Long memberId) {
        // ⚠️ 이미 slot 있는 애들까지 보내면 중복/덮어쓰기 위험 → null만 보내는 걸 추천
        List<FridgeIngredient> items =
                fridgeIngredientRepository.findAllByMemberIdAndFridgeSlotIsNullOrderByCreatedAtDesc(memberId);

        List<FastApiPlaceDtos.Item> reqItems = items.stream()
                .map(i -> new FastApiPlaceDtos.Item(
                        i.getId(),
                        i.getName(),
                        i.getQuantity(),
                        i.getStorageCategory()
                ))
                .toList();

        FastApiPlaceDtos.PlaceRes fastRes =
                fastApiClient.place(new FastApiPlaceDtos.PlaceReq(reqItems));

        // id로 빠르게 찾기
        Map<Long, FridgeIngredient> byId = items.stream()
                .collect(Collectors.toMap(FridgeIngredient::getId, x -> x));

        List<AutoPlaceDtos.PlacedItem> placed = new ArrayList<>();
        List<AutoPlaceDtos.UnplacedItem> unplaced = new ArrayList<>();

        if (fastRes != null && fastRes.placements() != null) {
            for (FastApiPlaceDtos.Placement p : fastRes.placements()) {
                FridgeIngredient ing = byId.get(p.id());
                if (ing == null) continue;

                FridgeSlot slot = FridgeSlot.valueOf(p.fridgeSlot());
                ing.assignSlot(slot);

                placed.add(new AutoPlaceDtos.PlacedItem(
                        ing.getId(),
                        ing.getName(),
                        ing.getStorageCategory(),
                        ing.getFridgeSlot().name()
                ));
            }
        }

        if (fastRes != null && fastRes.unplaced() != null) {
            for (FastApiPlaceDtos.Unplaced u : fastRes.unplaced()) {
                unplaced.add(new AutoPlaceDtos.UnplacedItem(
                        u.id(),
                        u.name(),
                        u.reason()
                ));
            }
        }

        return new AutoPlaceDtos.Res(placed, unplaced);
    }

    // ✅ OCR 찍고 → DB 저장 → place 호출 → slot까지 저장 → 결과 반환
    public FridgeDtos.OcrAutoPlaceRes ocrAutoPlace(Long memberId, MultipartFile image) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("member not found: " + memberId));

        // 1) FastAPI OCR(+정규화)
        FastApiOcrDtos.OcrRes ocrRes = fastApiClient.ocr(image);
        if (ocrRes == null || ocrRes.items() == null) {
            return FridgeDtos.OcrAutoPlaceRes.builder()
                    .rawText(ocrRes == null ? null : ocrRes.rawText())
                    .saved(List.of())
                    .placed(List.of())
                    .unplaced(List.of())
                    .build();
        }

        // 2) DB 저장 (slot null)
        List<FridgeIngredient> saved = new ArrayList<>();
        for (FastApiOcrDtos.OcrItem it : ocrRes.items()) {
            if (it == null) continue;

            FridgeIngredient entity = FridgeIngredient.builder()
                    .member(member)
                    .name(it.name())
                    .quantity(it.quantity())
                    .expiryDate(null)
                    .storageCategory(it.storageCategory())
                    .inputMethod(InputMethod.OCR)
                    .build();
            saved.add(entity);
        }
        fridgeIngredientRepository.saveAll(saved);

        // 3) place 호출(저장된 id 포함)
        List<FastApiPlaceDtos.Item> placeItems = saved.stream()
                .map(e -> new FastApiPlaceDtos.Item(
                        e.getId(),
                        e.getName(),
                        e.getQuantity(),
                        e.getStorageCategory()
                ))
                .toList();

        FastApiPlaceDtos.PlaceRes placeRes =
                fastApiClient.place(new FastApiPlaceDtos.PlaceReq(placeItems));

        Map<Long, FridgeIngredient> byId = saved.stream()
                .collect(Collectors.toMap(FridgeIngredient::getId, x -> x));

        List<FridgeDtos.PlacedItem> placedOut = new ArrayList<>();
        List<FridgeDtos.UnplacedItem> unplacedOut = new ArrayList<>();

        if (placeRes != null && placeRes.placements() != null) {
            for (FastApiPlaceDtos.Placement p : placeRes.placements()) {
                if (p == null || p.id() == null) continue;

                FridgeIngredient target = byId.get(p.id());
                if (target == null) continue;

                try {
                    FridgeSlot slot = FridgeSlot.valueOf(p.fridgeSlot());
                    target.assignSlot(slot);

                    placedOut.add(new FridgeDtos.PlacedItem(
                            target.getId(),
                            target.getName(),
                            target.getStorageCategory(),
                            slot.name()
                    ));
                } catch (Exception ignore) {
                    // 잘못된 enum이면 스킵
                }
            }
        }

        if (placeRes != null && placeRes.unplaced() != null) {
            for (FastApiPlaceDtos.Unplaced u : placeRes.unplaced()) {
                if (u == null) continue;
                unplacedOut.add(new FridgeDtos.UnplacedItem(u.id(), u.name(), u.reason()));
            }
        }

        fridgeIngredientRepository.saveAll(saved);

        return FridgeDtos.OcrAutoPlaceRes.builder()
                .rawText(ocrRes.rawText())
                .saved(saved.stream().map(this::toRes).toList())
                .placed(placedOut)
                .unplaced(unplacedOut)
                .build();
    }
}
