package codeshot.photogram.domain.member.domain.entity;


import codeshot.photogram.domain.login.domain.entity.LocalLogin;
import codeshot.photogram.domain.login.domain.LoginType;
import codeshot.photogram.domain.login.domain.entity.MemberTerm;
import codeshot.photogram.domain.login.domain.entity.SocialLogin;
import codeshot.photogram.domain.member.domain.Visibility;
import codeshot.photogram.domain.member.dto.MemberUpdateRequest;
import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseEntity implements UserDetails{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberID;

    @Column(unique = true)
    private String nickName;

    @Column
    private String name;

    @Column
    private String profileImageUrl;

    @Column
    private String backgroundImageUrl;

    @Enumerated(EnumType.STRING)
    private LoginType loginType;

    @Column
    private String introIndex;

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private LocalLogin localLogin;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SocialLogin socialLogin;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberTerm> memberTerms = new ArrayList<>();

    @Builder
    public Member(String nickName, String name, String profileImageUrl, String backgroundImageUrl,
                  LoginType loginType, String introIndex, Visibility visibility) {
        this.nickName = nickName;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.backgroundImageUrl = backgroundImageUrl;
        this.loginType = loginType;
        this.introIndex = introIndex;
        this.visibility = visibility;
        this.roles = new ArrayList<>(List.of("ROLE_USER"));
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "member_roles",
            joinColumns = @JoinColumn(name = "member_memberid", referencedColumnName = "memberid", nullable = false)
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<String> roles = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return String.valueOf(memberID); // JWT subject
    }

    @Override
    public String getPassword() {
        return null; // 비밀번호는 LocalLogin에서 관리
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void update(MemberUpdateRequest dto) {
        this.nickName = dto.getNickName();
        this.introIndex = dto.getIntroIndex();
        this.visibility = dto.getVisibility();
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }


}
