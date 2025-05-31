package codeshot.photogram.domain.comment.application;

import codeshot.photogram.domain.comment.domain.entity.Comment;
import codeshot.photogram.domain.comment.domain.repository.CommentRepository;
import codeshot.photogram.domain.comment.dto.CommentDTO;
import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.member.domain.repository.MemberRepository;
import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.post.domain.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService{

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;


    // 게시글에 댓글 작성
    @Override
    @Transactional
    public Comment addComment(Long postId, Long memberId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다."));

        Comment comment = Comment.createComment(member, post, content);
        return commentRepository.save(comment);
    }

    // 댓글에 대댓글 작성 (대댓글의 대댓글은 불가능)
    @Override
    @Transactional
    public Comment addReply(Long parentCommentId, Long memberId, String content) {
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new EntityNotFoundException("부모 댓글을 찾을 수 없습니다."));
        Post post = parentComment.getPost(); // 부모 댓글의 게시글 그대로 사용
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다."));

        if (parentComment.getParentComment() != null) {
            throw new IllegalArgumentException("대댓글의 대댓글은 허용되지 않습니다.");
        }

        Comment reply = Comment.createReply(member, post, parentComment, content);
        return commentRepository.save(reply);
    }

    /**
     * 댓글 작성 (게시글 기준)
     */
    @Override
    public CommentDTO writeComment(Long postId, Long memberId, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        Comment comment = Comment.createComment(member, post, content);
        commentRepository.save(comment);
        return CommentDTO.from(comment);
    }

    /**
     * 대댓글 작성 (댓글 기준)
     */
    @Override
    public CommentDTO writeReply(Long postId, Long memberId, Long parentCommentId, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        Comment parent = commentRepository.findById(parentCommentId).orElseThrow();

        Comment reply = Comment.createReply(member, post, parent, content);
        commentRepository.save(reply);
        return CommentDTO.from(reply);
    }

    /**
     * 계층형 댓글 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdAndParentCommentIsNull(postId);

        // 대댓글 포함하여 계층형으로 변환
        return comments.stream()
                .map(comment -> {
                    List<CommentDTO> replies = commentRepository.findByParentComment(comment).stream()
                            .map(CommentDTO::from)
                            .toList();
                    return CommentDTO.from(comment, replies);
                })
                .toList();
    }

    @Override
    @Transactional
    public CommentDTO updateComment(Long commentId, Long memberId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));

        if (!comment.getMember().getMemberID().equals(memberId)) {
            throw new IllegalArgumentException("댓글 작성자만 수정할 수 있습니다.");
        }

        comment.updateContent(newContent);
        return CommentDTO.from(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));

        if (!comment.getMember().getMemberID().equals(memberId)) {
            throw new IllegalArgumentException("댓글 작성자만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

}
