package codeshot.photogram.domain.hashtag.dto;

import codeshot.photogram.domain.post.domain.entity.PostImage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PostImageWithHashtags {
    private PostImage postImage;
    private List<String> hashtags;
}
