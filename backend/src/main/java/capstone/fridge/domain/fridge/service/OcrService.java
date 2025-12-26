package capstone.fridge.domain.fridge.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OcrService {
    List<OcrItem> extract(MultipartFile image);

    record OcrItem(String name, String quantity, String storageCategory) {}
}
