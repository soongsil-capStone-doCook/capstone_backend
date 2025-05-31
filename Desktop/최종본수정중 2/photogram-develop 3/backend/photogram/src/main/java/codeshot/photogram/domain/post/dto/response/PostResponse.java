package codeshot.photogram.domain.post.dto.response;

import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String content;
    private List<String> imageUrls; // 이미지 URL 목록 필드 추가
    private List<String> hashtags;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                // Post 엔티티의 images 리스트에서 imageUrl만 추출하여 DTO에 매핑
                .imageUrls(post.getImages() != null ? // NullPointerException 방지
                        post.getImages().stream()
                                .map(PostImage::getImageUrl) // PostImage 엔티티에 getImageUrl() 메서드가 있다고 가정
                                .collect(Collectors.toList()) : List.of()) // 이미지가 없으면 빈 리스트 반환
                .build();
    }

    public static PostResponse from(Post post, List<String> hashtags) {
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrls(post.getImages() != null ? // NullPointerException 방지
                        post.getImages().stream()
                                .map(PostImage::getImageUrl) // PostImage 엔티티에 getImageUrl() 메서드가 있다고 가정
                                .collect(Collectors.toList()) : List.of()) // 이미지가 없으면 빈 리스트 반환
                .hashtags(hashtags)
                .build();
    }
}