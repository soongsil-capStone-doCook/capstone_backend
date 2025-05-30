package codeshot.photogram.domain.post.application;

import codeshot.photogram.domain.hashtag.application.HashtagService;
import codeshot.photogram.domain.hashtag.domain.entity.Hashtag;
import codeshot.photogram.domain.hashtag.domain.entity.PostImageHashtag;
import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import codeshot.photogram.domain.post.domain.repository.PostImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional // 클래스 레벨에서 트랜잭션 관리 (기본적으로 readOnly = false)
public class PostImageServiceImpl implements PostImageService {

    private final PostImageRepository postImageRepository;
    private final PostService postService;
    private final S3Service s3Service;
    private final HashtagService hashtagService;

    /**
     * PostImage 업로드 + AI 해시태그 생성 후 저장 (게시글 연결)
     */
    @Override
    public PostImage uploadPostImageWithHashtags(List<MultipartFile> files, Long postId) {
        // 1. 게시글 조회
        Post post = postService.getPostById(postId);

        // 2. S3 업로드 -> 이미지 URL 획득 (여러 파일 처리)
        List<String> imageUrls = s3Service.uploadFiles(files);

        // 이미지 URL이 없는 경우 예외 처리 또는 로깅
        if (imageUrls.isEmpty()) {
            throw new RuntimeException("No images were uploaded.");
        }

        // 3. PostImage 생성 및 저장 (첫 번째 URL만 사용, 여러 PostImage 생성 고려)
        PostImage postImage = new PostImage();
        postImage.setImageUrl(imageUrls.get(0)); // 첫 번째 URL만 사용
        postImage.setPost(post);
        postImage = postImageRepository.save(postImage);

        // 4. AI 해시태그 생성
        List<String> extractedHashtags = hashtagService.extractHashtagsFromImage(imageUrls.get(0)); // 첫 번째 URL 사용

        // 5. PostImageHashtag 저장
        for (String tag : extractedHashtags) {
            Hashtag hashtag = hashtagService.createOrGet(tag);
            PostImageHashtag postImageHashtag = PostImageHashtag.create(postImage, hashtag);
            postImage.getPostImageHashtags().add(postImageHashtag);
        }

        return postImage;
    }

    /**
     * PostImage 업로드 + AI 해시태그 생성 후 저장 (게시글 연결 없이)
     */
    @Override
    public PostImage uploadPostImageWithHashtagsWithoutPost(List<MultipartFile> files) {
        // 1. S3 업로드 -> 이미지 URL 획득 (여러 파일 처리)
        List<String> imageUrls = s3Service.uploadFiles(files);

        // 이미지 URL이 없는 경우 예외 처리 또는 로깅
        if (imageUrls.isEmpty()) {
            throw new RuntimeException("No images were uploaded.");
        }

        // 2. PostImage 생성 및 저장 (첫 번째 URL만 사용, 여러 PostImage 생성 고려)
        PostImage postImage = new PostImage();
        postImage.setImageUrl(imageUrls.get(0)); // 첫 번째 URL만 사용
        postImage = postImageRepository.save(postImage);

        // 3. AI 해시태그 생성
        List<String> extractedHashtags = hashtagService.extractHashtagsFromImage(imageUrls.get(0)); // 첫 번째 URL 사용

        // 4. PostImageHashtag 저장
        for (String tag : extractedHashtags) {
            Hashtag hashtag = hashtagService.createOrGet(tag);
            PostImageHashtag postImageHashtag = PostImageHashtag.create(postImage, hashtag);
            postImage.getPostImageHashtags().add(postImageHashtag);
        }

        return postImage;
    }
}