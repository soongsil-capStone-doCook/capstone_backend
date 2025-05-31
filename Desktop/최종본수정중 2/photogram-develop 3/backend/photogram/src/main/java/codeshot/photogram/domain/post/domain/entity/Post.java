package codeshot.photogram.domain.post.domain.entity;

// import codeshot.photogram.domain.hashtag.domain.entity.Hashtag; // ⚠️ 삭제
import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Column(name = "memberid")
    private Long memberId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "area")
    private String area;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "likes")
    private Integer likes;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>(); // 초기화 (필요시)

    // ⚠️ 아래 @ManyToMany 관계와 필드를 완전히 삭제합니다.
    /*
    @ManyToMany
    @JoinTable(
            name = "post_hashtag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    private List<Hashtag> hashtags;
    */

    @PrePersist
    public void prePersist() {
        this.createdAt = this.createdAt == null ? LocalDateTime.now() : this.createdAt;
        this.likes = this.likes == null ? 0 : this.likes;
        // this.images = this.images == null ? new ArrayList<>() : this.images; // 이미 선언 시 초기화되어 있다면 필요 없음
        // ⚠️ this.hashtags = this.hashtags == null ? new ArrayList<>() : this.hashtags; // 이 부분도 삭제합니다.
    }

    public void setImages(List<PostImage> images) {
        this.images = images;
        images.forEach(image -> image.setPost(this));
    }

    public void setArea(String area) {
        this.area = area;
    }

    // Builder 패턴 사용 시 @Builder.Default 어노테이션으로 초기화하는 것이 더 깔끔할 수 있습니다.
    // 예: @Builder.Default private List<PostImage> images = new ArrayList<>();
    // @PrePersist는 주로 createdAt, likes와 같은 기본값 설정에 사용합니다.
}