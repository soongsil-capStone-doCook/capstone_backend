package codeshot.photogram.domain.login.domain.entity;

import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class LocalLogin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String photogramId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public LocalLogin(String email, String password, String photogramId, Member member) {
        this.email = email;
        this.password = password;
        this.photogramId = photogramId;
        this.member = member;
    }
}

