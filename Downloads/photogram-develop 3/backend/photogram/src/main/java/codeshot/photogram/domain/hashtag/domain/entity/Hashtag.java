package codeshot.photogram.domain.hashtag.domain.entity;

import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hashtag")
@Getter @Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // ✅ 이 어노테이션을 추가합니다.
public class Hashtag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hashtag_id")
    private Long id;

    @Column(name = "hashtag_name", unique = true, length = 200, nullable = false)
    private String name;

    @OneToMany(mappedBy = "hashtag", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostImageHashtag> postImageHashtags = new ArrayList<>();

    // 만약 `Hashtag(String name)` 생성자가 필요하다면 이 `@AllArgsConstructor`와 충돌할 수 있습니다.
    // 하지만 빌더를 사용한다면 이 생성자는 불필요합니다.
}