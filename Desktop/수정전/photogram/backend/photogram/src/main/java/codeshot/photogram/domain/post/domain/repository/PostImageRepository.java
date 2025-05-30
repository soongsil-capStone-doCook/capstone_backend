package codeshot.photogram.domain.post.domain.repository;

import codeshot.photogram.domain.post.domain.entity.Post;  // Post 클래스 import 추가
import codeshot.photogram.domain.post.domain.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    @Query("SELECT pi FROM PostImage pi " +
            "JOIN pi.postImageHashtags pih " +
            "JOIN pih.hashtag h " +
            "WHERE h.name IN :hashtags " +
            "GROUP BY pi " +
            "HAVING COUNT(DISTINCT h.name) = :tagCount")
    List<PostImage> findByAllHashtagsMatch(@Param("hashtags") List<String> hashtags,
                                           @Param("tagCount") Long tagCount);

    void deleteByPost(Post post);
}