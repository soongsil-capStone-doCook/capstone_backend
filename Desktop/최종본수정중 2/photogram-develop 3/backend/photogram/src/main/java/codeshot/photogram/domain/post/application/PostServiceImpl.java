package codeshot.photogram.domain.post.application;

import codeshot.photogram.domain.hashtag.domain.entity.Hashtag;
import codeshot.photogram.domain.hashtag.domain.entity.PostImageHashtag;
import codeshot.photogram.domain.hashtag.domain.repository.HashtagRepository;
import codeshot.photogram.domain.hashtag.domain.repository.PostImageHashtagRepository;
import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import codeshot.photogram.domain.post.domain.repository.PostImageRepository;
import codeshot.photogram.domain.post.domain.repository.PostRepository;
import codeshot.photogram.domain.post.dto.request.PostCreateRequest;
import codeshot.photogram.domain.post.dto.request.UpdatePostRequest;
import codeshot.photogram.domain.shot.domain.repository.ShotRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final HashtagRepository hashtagRepository;
    private final ShotRepository shotRepository;
    private final PostImageHashtagRepository postImageHashtagRepository;

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
    public List<Post> getAllPosts() {
        // 해시태그와 이미지를 모두 페치 조인하여 N+1 문제 없이 모든 Post 조회
        return postRepository.findAllWithImagesAndHashtags();
    }

    @Override
    @Transactional
    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Pair<Post, List<String>> updatePost(Long id, UpdatePostRequest request, Long memberId) {
        Post post = getPostById(id);

        if (!post.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("게시글을 수정할 권한이 없습니다.");
        }

        try {
            // 게시글 내용 수정 (NPE 방지)
            post.setContent(request.getContent() != null ? request.getContent() : "");
            post.setArea(request.getArea() != null ? request.getArea() : "");

            // 기존 이미지와 해시태그 삭제
            List<PostImage> oldImages = new ArrayList<>(post.getImages());
            for (PostImage image : oldImages) {
                postImageHashtagRepository.deleteByPostImage(image);
            }
            postImageRepository.deleteByPost(post);

            // 새로운 해시태그 준비
            List<Hashtag> associatedHashtags = new ArrayList<>();
            if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
                for (String tagName : request.getHashtags()) {
                    if (tagName == null) continue;
                    
                    String cleanedTagName = tagName.startsWith("#") ? tagName.substring(1) : tagName;
                    cleanedTagName = cleanedTagName.trim();

                    if (!cleanedTagName.isEmpty()) {
                        String finalCleanedTagName = cleanedTagName;
                        Hashtag hashtag = hashtagRepository.findByName(cleanedTagName)
                                .orElseGet(() -> {
                                    Hashtag newHashtag = Hashtag.builder()
                                            .name(finalCleanedTagName)
                                            .build();
                                    return hashtagRepository.save(newHashtag);
                                });
                        associatedHashtags.add(hashtag);
                    }
                }
            }

            // 새 이미지 생성 및 해시태그 연결
            List<PostImage> newImages = new ArrayList<>();
            if (request.getImageUrls() != null) {
                for (String url : request.getImageUrls()) {
                    if (url == null || url.trim().isEmpty()) continue;

                    PostImage postImage = new PostImage();
                    postImage.setImageUrl(url);
                    postImage.setPost(post);

                    List<PostImageHashtag> imageHashtags = new ArrayList<>();
                    for (Hashtag hashtag : associatedHashtags) {
                        PostImageHashtag pih = PostImageHashtag.create(postImage, hashtag);
                        imageHashtags.add(pih);
                    }
                    postImage.setPostImageHashtags(imageHashtags);
                    newImages.add(postImage);
                }
            }

            // 이미지 컬렉션 업데이트
            post.getImages().clear();  // 기존 이미지 컬렉션 비우기
            post.getImages().addAll(newImages);  // 새 이미지 추가

            // 변경 사항 저장
            Post saved = postRepository.save(post);  // saveAndFlush 대신 save 사용
            
            // 저장된 해시태그 이름 목록 반환
            List<String> hashtagNames = associatedHashtags.stream()
                .map(Hashtag::getName)
                .collect(Collectors.toList());

            return Pair.of(saved, hashtagNames);
            
        } catch (Exception e) {
            throw new RuntimeException("게시글 수정 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
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