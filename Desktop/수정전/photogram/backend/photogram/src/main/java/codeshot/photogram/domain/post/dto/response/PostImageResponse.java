package codeshot.photogram.domain.post.dto.response;

import codeshot.photogram.domain.hashtag.domain.entity.PostImageHashtag;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class PostImageResponse {
    private Long postImageId;
    private String imageUrl;
    private List<String> hashtags;  // 해시태그 이름 리스트

    public static PostImageResponse from(PostImage postImage) {
        List<String> hashtags = postImage.getPostImageHashtags().stream()
                .map(PostImageHashtag::getHashtag)
                .map(h -> "#" + h.getName())  // 해시태그 형태로 반환
                .collect(Collectors.toList());

        return PostImageResponse.builder()
                .postImageId(postImage.getId())
                .imageUrl(postImage.getImageUrl())
                .hashtags(hashtags)
                .build();
    }
}
