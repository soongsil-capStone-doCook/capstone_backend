package codeshot.photogram.domain.post.api;

import codeshot.photogram.domain.post.application.PostImageService;
import codeshot.photogram.domain.post.application.PostService;
import codeshot.photogram.domain.post.application.S3Service;
import codeshot.photogram.domain.post.domain.entity.Post;
import org.springframework.http.HttpStatus;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import codeshot.photogram.domain.post.dto.request.PostCreateRequest;
import codeshot.photogram.domain.post.dto.request.UpdatePostRequest;
import codeshot.photogram.domain.post.dto.response.PostImageResponse;
import codeshot.photogram.domain.post.dto.response.PostResponse;
import codeshot.photogram.domain.post.dto.response.S3UploadResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/histories")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final S3Service s3Service;
    private final PostImageService postImageService;

    // 1. 게시글 생성
    @PostMapping("/posts")
    public ResponseEntity<Map<String, Long>> createPost(@RequestBody PostCreateRequest request,
                                                        @AuthenticationPrincipal(expression = "username") String memberIdStr) {
        Long memberId = Long.valueOf(memberIdStr);
        Post savedPost = postService.createPostWithImageUrls(request, memberId);
        return ResponseEntity.ok(Map.of("postId", savedPost.getId()));
    }

    // 2. 특정 지역(area)별 게시글 조회 또는 전체 게시글 조회
    // 이 엔드포인트는 "/histories" (컨트롤러 기본 경로)에 매핑됩니다.
    @GetMapping
public ResponseEntity<List<PostResponse>> getAreaPosts(@RequestParam(required = false) String area) {
    List<Post> posts;
    if (area != null && !area.isEmpty()) {
        posts = postService.getPostsByArea(area);
    } else {
        posts = postService.getAllPostsNoPaging();
    }

    List<PostResponse> response = posts.stream()
            .map(post -> PostResponse.from(post, extractHashtags(post)))
            .collect(Collectors.toList());

    return ResponseEntity.ok(response);
}

    @GetMapping("/userpage")
public ResponseEntity<List<PostResponse>> getUserPosts(
        @RequestParam String area,
        @AuthenticationPrincipal(expression = "username") String memberIdStr
) {
    Long memberId = Long.valueOf(memberIdStr);
    List<Post> posts = postService.getUserPostsByArea(area, memberId);

    List<PostResponse> response = posts.stream()
            .map(post -> PostResponse.from(post, extractHashtags(post)))
            .collect(Collectors.toList());

    return ResponseEntity.ok(response);
}

   @GetMapping("/filter/{postId}")
public ResponseEntity<PostResponse> getPostById(@PathVariable Long postId) {
    try {
        Post post = postService.getPostById(postId);
        return ResponseEntity.ok(PostResponse.from(post, extractHashtags(post)));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null);
    }
}



    // 4. 게시글 수정
   @PutMapping("/newpost/{postId}")
public ResponseEntity<PostResponse> updatePost(@PathVariable Long postId,
                                               @AuthenticationPrincipal(expression = "username") String memberIdStr,
                                               @RequestBody UpdatePostRequest request) {
    Long memberId = Long.valueOf(memberIdStr);
    Pair<Post, List<String>> result = postService.updatePost(postId, request, memberId);
    return ResponseEntity.ok(PostResponse.from(result.getLeft(), result.getRight()));
}

    // 5. 게시글 삭제
    @DeleteMapping("/delete/{postId}")
    public ResponseEntity<String> deletePost(@PathVariable Long postId,
                                             @AuthenticationPrincipal(expression = "username") String memberIdStr) {
        Long memberId = Long.valueOf(memberIdStr);
        postService.deletePost(postId, memberId);
        return ResponseEntity.ok("삭제 완료");
    }

    // 6. S3 직접 이미지 업로드 (Presigned URL 사용)
    @PostMapping("/images/upload")
    public ResponseEntity<S3UploadResponse> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = s3Service.uploadFiles(files); // ✅ s3Service.uploadFile -> s3Service.uploadFiles 호출
        return ResponseEntity.ok(new S3UploadResponse(urls)); // ✅ DTO에 List<String>을 넘겨줍니다.
    }

    // 7. 게시글에 PostImage + 해시태그 자동 생성 업로드
    @PostMapping("/{postId}/images")
    public ResponseEntity<PostImageResponse> uploadPostImage(@PathVariable Long postId,
                                                             @RequestParam("file") List<MultipartFile> files) {
        PostImage postImage = postImageService.uploadPostImageWithHashtags(files, postId);
        return ResponseEntity.ok(PostImageResponse.from(postImage));
    }

    // 8. 게시글과 연결되지 않은 PostImage + 해시태그 자동 생성 업로드 (새 게시글 작성 시 임시 이미지 업로드용)
    @PostMapping("/images/upload-with-hashtags")
    public ResponseEntity<PostImageResponse> uploadImageWithHashtags(@RequestParam("file") List<MultipartFile> files) {
        PostImage postImage = postImageService.uploadPostImageWithHashtagsWithoutPost(files);
        return ResponseEntity.ok(PostImageResponse.from(postImage));
    }

    private List<String> extractHashtags(Post post) {
    return post.getImages().stream()
            .flatMap(img -> img.getPostImageHashtags().stream())
            .map(pih -> pih.getHashtag().getName())
            .distinct()
            .collect(Collectors.toList());
}

 @GetMapping("/allposts")
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();

        List<PostResponse> responses = posts.stream().map(post -> {
            // 각 Post에 대한 해시태그 수집
            List<String> hashtags = post.getImages().stream()
                    .flatMap(image -> image.getPostImageHashtags().stream())
                    .map(assoc -> assoc.getHashtag().getName())
                    .distinct()
                    .collect(Collectors.toList());

            // Post + 해시태그를 DTO로 변환
            return PostResponse.from(post, hashtags);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}