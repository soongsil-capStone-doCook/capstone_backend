package codeshot.photogram.domain.comment.dto;

import codeshot.photogram.domain.comment.domain.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter @Setter
public class CommentDTO {

    private Long commentId;
    private Long postId;
    private Long memberId;
    private String memberName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Comment ParentComment;
    private List<CommentDTO> replies;

    public CommentDTO (String content){
        this.content = content;
    }

    /**
     * 계층형 댓글 DTO 변환 (리플 포함)
     */
    public static CommentDTO from(Comment comment, List<CommentDTO> replies) {

        return CommentDTO.builder()
                .commentId(comment.getId())
                .postId(comment.getPost().getId())
                .memberId(comment.getMember().getMemberID())
                .memberName(comment.getMember().getNickName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .ParentComment(comment.getParentComment())
                .replies(replies)
                .build();
    }

    /**
     * 기본 댓글 DTO 변환 (리플 없이)
     */
    public static CommentDTO from(Comment comment) {
        return from(comment, null);
    }
}
