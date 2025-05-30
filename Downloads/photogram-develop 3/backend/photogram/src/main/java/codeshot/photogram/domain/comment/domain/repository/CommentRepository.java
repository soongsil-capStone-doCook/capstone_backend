package codeshot.photogram.domain.comment.domain.repository;

import codeshot.photogram.domain.comment.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdAndParentCommentIsNull(Long postId);
    List<Comment> findByParentComment(Comment parentComment);
}

