package codeshot.photogram.domain.comment.application;

import codeshot.photogram.domain.comment.domain.entity.Comment;
import codeshot.photogram.domain.comment.dto.CommentDTO;

import java.util.List;

public interface CommentService {

    Comment addComment(Long postId, Long memberId, String content);
    Comment addReply(Long parentCommentId, Long memberId, String content);
    CommentDTO writeComment(Long postId, Long memberId, String content);
    CommentDTO writeReply(Long postId, Long memberId, Long parentCommentId, String content);
    List<CommentDTO> getCommentsByPost(Long postId);
    CommentDTO updateComment(Long commentId, Long memberId, String newContent);
    void deleteComment(Long commentId, Long memberId);
}
