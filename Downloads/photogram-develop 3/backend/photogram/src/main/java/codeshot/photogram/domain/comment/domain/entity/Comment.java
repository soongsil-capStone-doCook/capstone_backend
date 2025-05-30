package codeshot.photogram.domain.comment.domain.entity;

import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();

    private String content;

    // 팩토리 메서드들
    public static Comment createComment(Member member, Post post, String content) {
        Comment comment = new Comment();
        comment.member = member;
        comment.post = post;
        comment.content = content;
        return comment;
    }

    public static Comment createReply(Member member, Post post, Comment parentComment, String content) {
        Comment reply = new Comment();
        reply.member = member;
        reply.post = post;
        reply.parentComment = parentComment;
        reply.content = content;
        return reply;
    }

    public void updateContent(String newContent) {
        this.content = newContent;
    }

}

