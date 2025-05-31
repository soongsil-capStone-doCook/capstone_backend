package codeshot.photogram.domain.post.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePostRequest {
    private String content;
    private List<String> imageUrls;
    private List<String> hashtags;
    private String area;
}
