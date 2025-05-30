package codeshot.photogram.domain.post.application;

import codeshot.photogram.domain.hashtag.domain.entity.Hashtag;
import codeshot.photogram.domain.hashtag.domain.entity.PostImageHashtag;
import codeshot.photogram.domain.hashtag.domain.repository.HashtagRepository;
import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import codeshot.photogram.domain.post.domain.repository.PostImageRepository;
import codeshot.photogram.domain.post.domain.repository.PostRepository;
import codeshot.photogram.domain.post.dto.request.PostCreateRequest;
import codeshot.photogram.domain.hashtag.domain.repository.PostImageHashtagRepository;  // 추가
import codeshot.photogram.domain.post.dto.request.UpdatePostRequest;
import codeshot.photogram.domain.shot.domain.repository.ShotRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final HashtagRepository hashtagRepository;
    private final ShotRepository shotRepository;
    private final PostImageHashtagRepository postImageHashtagRepository;  // 추가


    public PostServiceImpl(
            PostRepository postRepository,
            PostImageRepository postImageRepository,
            HashtagRepository hashtagRepository,
            ShotRepository shotRepository,
            PostImageHashtagRepository postImageHashtagRepository  // 추가
    ) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.hashtagRepository = hashtagRepository;
        this.shotRepository = shotRepository;
        this.postImageHashtagRepository = postImageHashtagRepository;  // 추가
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Post getPostById(Long id) {
        // ✅ 기존의 getPostById 메서드를 이 부분으로 수정해야 합니다.
        // findById 대신 findByIdWithImages를 사용하도록 변경
        return postRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<Post> getAllPosts(Pageable pageable) {
        // 기존 페이징 조회 메서드. 이미지 로딩은 PostResponse에서 처리.
        return postRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post updatePost(Long id, UpdatePostRequest request, Long memberId) {
        Post post = getPostById(id);
        if (!post.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("게시글을 수정할 권한이 없습니다.");
        }
        post.setContent(request.getContent());

        return postRepository.save(post);
    }

    @Override
    @Transactional
    public void deletePost(Long id, Long memberId) {
        Post post = getPostById(id);
        if (!post.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("게시글을 삭제할 권한이 없습니다.");
        }

        // 1. PostImage와 관련된 PostImageHashtag 삭제
        for (PostImage image : post.getImages()) {
            postImageHashtagRepository.deleteByPostImage(image);
        }

        // 2. PostImage 삭제
        postImageRepository.deleteByPost(post);

        // 3. Shot 삭제 (필요한 경우)
        shotRepository.deleteById(id);

        // 4. 마지막으로 Post 삭제
        postRepository.deleteById(id);
    }

    @Override
    public Page<Post> getPostsByTags(Long userId, List<String> tags, long size, Pageable pageable) {
        // TODO: 태그 기반 조회 로직 구현
        return null;
    }

    @Override
    public Page<Post> getAllPost(Pageable pageable) {
        // TODO: 다른 getAllPost 메서드와의 중복 여부 확인 및 사용처에 따라 구현 또는 제거
        return null;
    }

    @Override
    @Transactional
    public Post createPostWithImageUrls(PostCreateRequest request, Long memberId) {
        Post post = new Post();
        post.setContent(request.getContent());

        // 1. 해시태그 엔티티들을 미리 준비 (요청의 모든 태그를 처리)
        List<Hashtag> associatedHashtags = new ArrayList<>();
        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            for (String tagName : request.getHashtags()) {
                String cleanedTagName = tagName.startsWith("#") ? tagName.substring(1) : tagName;
                cleanedTagName = cleanedTagName.trim();

                if (!cleanedTagName.isEmpty()) {
                    String finalCleanedTagName = cleanedTagName;
                    Hashtag hashtag = hashtagRepository.findByName(cleanedTagName)
                            .orElseGet(() -> {
                                Hashtag newHashtag = Hashtag.builder()
                                        .name(finalCleanedTagName) // name 필드를 설정
                                        .build();
                                hashtagRepository.save(newHashtag); // 새로 생성된 Hashtag는 반드시 저장
                                return newHashtag;
                            });
                    associatedHashtags.add(hashtag);
                }
            }
        }
        // Post 엔티티에는 이제 hashtags 필드가 없으므로 post.setHashtags()는 호출하지 않습니다.


        // 2. PostImage 객체 생성 및 PostImageHashtag 연결
        List<PostImage> images = request.getImageUrls().stream().map(url -> {
            PostImage postImage = new PostImage();
            postImage.setImageUrl(url);
            postImage.setPost(post); // Post와 PostImage 연결

            // 각 PostImage에 모든 준비된 해시태그를 연결
            List<PostImageHashtag> imageHashtags = new ArrayList<>();
            for (Hashtag hashtag : associatedHashtags) {
                // PostImageHashtag::create 정적 팩토리 메서드 사용
                PostImageHashtag pih = PostImageHashtag.create(postImage, hashtag);
                imageHashtags.add(pih);
            }
            postImage.setPostImageHashtags(imageHashtags); // PostImage에 PostImageHashtag 리스트 설정

            return postImage;
        }).collect(Collectors.toList());

        post.setImages(images); // Post 엔티티에 이미지 리스트 설정

        post.setMemberId(memberId);

        // 3. Post 저장 (CascadeType.ALL에 의해 PostImage 및 PostImageHashtag도 함께 저장됨)
        return postRepository.save(post);
    }


    // ✅ 이 아래에 있던 두 번째 getPostById 메서드를 삭제해야 합니다.
    // --- 새로 구현하는 메서드 ---

    @Override
    @Transactional(Transactional.TxType.SUPPORTS) // 읽기 전용
    public List<Post> getAllPostsNoPaging() {
        return postRepository.findAllWithImages();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS) // 읽기 전용
    public List<Post> getPostsByArea(String area) {
        return postRepository.findByAreaWithImages(area);
    }

    @Override
    @Transactional
    public List<Post> getUserPostsByArea(String area, Long memberId) {
        return postRepository.findByUserAreaWithImages(area, memberId);
    }
}