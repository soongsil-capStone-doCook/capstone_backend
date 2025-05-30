package codeshot.photogram.domain.login.domain.repository;

import codeshot.photogram.domain.login.domain.SocialProvider;
import codeshot.photogram.domain.login.domain.entity.SocialLogin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialLoginRepository extends JpaRepository<SocialLogin, Long> {

    @EntityGraph(attributePaths = {"member", "member.roles"})
    Optional<SocialLogin> findByProviderAndProviderId(SocialProvider provider, String providerId);
}

