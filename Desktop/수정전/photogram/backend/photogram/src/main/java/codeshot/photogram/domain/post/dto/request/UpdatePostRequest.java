package codeshot.photogram.domain.post.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UpdatePostRequest {
    private String content;
    private List<String> hashtags;
}
