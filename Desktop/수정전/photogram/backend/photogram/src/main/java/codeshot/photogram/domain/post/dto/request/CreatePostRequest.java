package codeshot.photogram.domain.post.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CreatePostRequest {
    private String content;
    private List<String> hashtags;
}
