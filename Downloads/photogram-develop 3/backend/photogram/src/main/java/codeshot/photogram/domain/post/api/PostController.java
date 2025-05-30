package codeshot.photogram.domain.post.api;

import codeshot.photogram.domain.post.application.PostImageService;
import codeshot.photogram.domain.post.application.PostService;
import codeshot.photogram.domain.post.application.S3Service;
import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import codeshot.photogram.domain.post.dto.request.PostCreateRequest;
import codeshot.photogram.domain.post.dto.request.UpdatePostRequest;
import codeshot.photogram.domain.post.dto.response.PostImageResponse;
import codeshot.photogram.domain.post.dto.response.PostResponse;
import codeshot.photogram.domain.post.dto.response.S3UploadResponse;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<List<PostResponse>> getAreaPosts(
            @RequestParam(required = false) String area
    ) {
        List<Post> posts;
        if (area != null && !area.isEmpty()) {
            // area 파라미터가 있을 경우 해당 area(해시태그 이름으로 가정)로 게시글 필터링
            // PostService의 getPostsByArea 메서드를 호출합니다.
            posts = postService.getPostsByArea(area);
        } else {
            // area 파라미터가 없을 경우 모든 게시글을 페이징 없이 조회합니다.
            // PostService의 getAllPostsNoPaging 메서드를 호출합니다.
            posts = postService.getAllPostsNoPaging();
        }

        // 조회된 Post 엔티티 리스트를 PostResponse DTO 리스트로 변환
        List<PostResponse> response = posts.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/userpage")
    public ResponseEntity<List<PostResponse>> getUserPosts(
            @RequestParam String area,
            @AuthenticationPrincipal(expression = "username") String memberIdStr
    ) {
        List<Post> posts;
        Long memberId = Long.valueOf(memberIdStr);

        posts = postService.getUserPostsByArea(area, memberId);

        // 조회된 Post 엔티티 리스트를 PostResponse DTO 리스트로 변환
        List<PostResponse> response = posts.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // 3. 특정 게시글 조회
    @GetMapping("/filter/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        return ResponseEntity.ok(PostResponse.from(post));
    }

    // 4. 게시글 수정
    @PutMapping("/newpost/{postId}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long postId,
                                                   @AuthenticationPrincipal(expression = "username") String memberIdStr,
                                                   @RequestBody UpdatePostRequest request) {
        Long memberId = Long.valueOf(memberIdStr);
        Post updated = postService.updatePost(postId, request, memberId);
        return ResponseEntity.ok(PostResponse.from(updated));
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
}