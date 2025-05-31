package codeshot.photogram.domain.comment.api;

import codeshot.photogram.domain.comment.application.CommentService;
import codeshot.photogram.domain.comment.dto.CommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/histories")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 게시글에 댓글 작성
    @PostMapping("/comments/{postId}")
    public ResponseEntity<CommentDTO> createComment(@PathVariable Long postId,
                                                    @AuthenticationPrincipal(expression = "username") String memberIdStr,
                                                    @RequestBody CommentDTO content) {
        Long memberId = Long.valueOf(memberIdStr);
        System.out.printf("content : " + content.getContent());
        CommentDTO commentDTO = commentService.writeComment(postId, memberId, content.getContent());
        return ResponseEntity.ok(commentDTO);
    }

    // 댓글에 대댓글 작성
    @PostMapping("/{postId}/reply/{parentCommentId}")
    public ResponseEntity<CommentDTO> createReply(@PathVariable Long postId,
                                                  @PathVariable Long parentCommentId,
                                                  @AuthenticationPrincipal(expression = "username") String memberIdStr,
                                                  @RequestBody CommentDTO content) {
        Long memberId = Long.valueOf(memberIdStr);
        CommentDTO replyDTO = commentService.writeReply(postId, memberId, parentCommentId, content.getContent());
        return ResponseEntity.ok(replyDTO);
    }

    // 게시글 기준 댓글 + 대댓글 조회 (계층형)
    @GetMapping("/{postId}/all")
    public ResponseEntity<List<CommentDTO>> getAllComments(@PathVariable Long postId) {
        List<CommentDTO> comments = commentService.getCommentsByPost(postId);
        return ResponseEntity.ok(comments);
    }

    // 댓글 또는 대댓글 수정
    @PutMapping("/newcomment/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(@PathVariable Long commentId,
                                                    @AuthenticationPrincipal(expression = "username") String memberIdStr,
                                                    @RequestBody CommentDTO newContent) {
        Long memberId = Long.valueOf(memberIdStr);
        CommentDTO updatedComment = commentService.updateComment(commentId, memberId, newContent.getContent());
        return ResponseEntity.ok(updatedComment);
    }

    // 댓글 또는 대댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId,
                                              @AuthenticationPrincipal(expression = "username") String memberIdStr) {
        Long memberId = Long.valueOf(memberIdStr);
        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.noContent().build();
    }

}
