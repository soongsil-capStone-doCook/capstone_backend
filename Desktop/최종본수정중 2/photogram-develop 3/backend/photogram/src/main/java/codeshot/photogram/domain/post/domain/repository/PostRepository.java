package codeshot.photogram.domain.post.domain.repository;

import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // Post ID로 Post와 해당 PostImage를 함께 Eager 로딩하는 쿼리
    // PostServiceImpl의 getPostById에서 사용됩니다.
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Post> findByIdWithImages(@Param("id") Long id);

    // ⭐️ 중요: Post 엔티티에서 hashtags 필드가 제거되었으므로,
    // Post의 hashtags 필드를 직접 참조하는 아래 쿼리들은 더 이상 유효하지 않습니다.
    // 이 쿼리들이 필요하다면, PostImageHashtag를 통해 관계를 다시 정의해야 합니다.
    // 현재는 이 두 쿼리를 제거하거나 주석 처리하는 것이 맞습니다.

    @Query("SELECT pi FROM PostImage pi " +
            "JOIN pi.postImageHashtags pih " +
            "JOIN pih.hashtag h " +
            "WHERE h.name IN :hashtags " +
            "GROUP BY pi " +
            "HAVING COUNT(DISTINCT h.name) = :hashtagCount")
    Page<PostImage> findByHashtags(@Param("hashtags") List<String> hashtags, @Param("hashtagCount") long hashtagCount, Pageable pageable);


    @Query("SELECT pi FROM PostImage pi " +
            "JOIN pi.postImageHashtags pih " +
            "JOIN pih.hashtag h " +
            "JOIN pi.post p " +
            "WHERE h.name IN :hashtags AND p.memberId = :memberId " +
            "GROUP BY pi " +
            "HAVING COUNT(DISTINCT h.name) = :hashtagCount")
    Page<PostImage> findByUserHashtags(@Param("hashtags") List<String> hashtags, @Param("memberId") Long memberId,
                                       @Param("hashtagCount") long hashtagCount, Pageable pageable);


    // --- 새로 추가하는 메서드들 ---

    // 1. 모든 Post와 관련된 PostImage를 함께 Eager 로딩하는 쿼리 (N+1 문제 방지)
    // Post의 images 필드가 @OneToMany 관계이므로 FETCH JOIN을 사용하여 한 번의 쿼리로 데이터를 가져옵니다.
    // 만약 해시태그까지 한 번에 가져오고 싶다면 PostImageHashtag와 Hashtag까지 FETCH JOIN을 추가해야 합니다.
    @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.images")
    List<Post> findAllWithImages();

    // 2. 특정 area(해시태그 이름으로 가정)에 해당하는 Post와 PostImage, Hashtag를 함께 Eager 로딩하는 쿼리
    // ✅ 이전에 발생했던 오류를 해결하기 위해 쿼리 경로를 수정했습니다.
    // Post -> PostImage -> PostImageHashtag -> Hashtag 순서로 조인합니다.
    @Query("SELECT DISTINCT p FROM Post p " +
            "JOIN FETCH p.images pi " + // Post와 PostImage 조인 (N+1 방지)
            "JOIN pi.postImageHashtags pih " + // PostImage와 PostImageHashtag 조인
            "JOIN pih.hashtag h " + // PostImageHashtag와 Hashtag 조인
            "WHERE h.name = :area") // Hashtag의 name으로 조건 필터링
    List<Post> findByAreaWithImages(@Param("area") String area);

    @Query("SELECT DISTINCT p FROM Post p " +
            "JOIN FETCH p.images pi " + // Post와 PostImage 조인 (N+1 방지)
            "JOIN pi.postImageHashtags pih " + // PostImage와 PostImageHashtag 조인
            "JOIN pih.hashtag h " + // PostImageHashtag와 Hashtag 조인
            "WHERE h.name = :area AND p.memberId = :memberId") // 특정 멤버 필터링
    List<Post> findByUserAreaWithImages(@Param("area") String area, @Param("memberId") Long memberId);
    
    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.images i " +
            "LEFT JOIN FETCH i.postImageHashtags ph " +
            "LEFT JOIN FETCH ph.hashtag")
    List<Post> findAllWithImagesAndHashtags();
}
