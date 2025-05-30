package codeshot.photogram.domain.shot.domain.entity;

import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.post.domain.entity.Post;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@Entity
@Table(name = "likes") // ERD에 따라
public class Shot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @JoinColumn(name = "liked")
    private boolean liked;
}
