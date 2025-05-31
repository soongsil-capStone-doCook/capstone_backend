package codeshot.photogram.domain.hashtag.domain.repository;

import codeshot.photogram.domain.hashtag.domain.entity.PostImageHashtag;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageHashtagRepository extends JpaRepository<PostImageHashtag, Long> {
    void deleteByPostImage(PostImage postImage);
}