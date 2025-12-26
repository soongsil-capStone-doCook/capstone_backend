package capstone.fridge.domain.fridge.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Profile("dev")
public class DummyOcrService implements OcrService {

    @Override
    public List<OcrItem> extract(MultipartFile image) {
        return List.of(
                new OcrItem("egg", "10", "유제품"),
                new OcrItem("pork", "300g", "육류"),
                new OcrItem("lettuce", "1", "채소")
        );
    }
}
