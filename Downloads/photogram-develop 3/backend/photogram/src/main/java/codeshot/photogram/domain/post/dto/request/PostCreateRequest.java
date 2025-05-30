package codeshot.photogram.domain.post.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostCreateRequest {
    private String content;
    private List<String> imageUrls; // ✅ 여러 이미지 URL
    private List<String> hashtags;
}

