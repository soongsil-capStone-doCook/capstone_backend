package codeshot.photogram.domain.login.domain.entity;

import codeshot.photogram.domain.login.domain.SocialProvider;
import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class SocialLogin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String email;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public SocialLogin(String email, String providerId, SocialProvider provider, Member member) {
        this.email = email;
        this.providerId = providerId;
        this.provider = provider;
        this.member = member;
    }
}

