package codeshot.photogram.domain.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class S3UploadResponse {
    private List<String> imageUrls;
}
