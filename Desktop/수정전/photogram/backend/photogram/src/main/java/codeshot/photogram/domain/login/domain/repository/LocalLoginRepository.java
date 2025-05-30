package codeshot.photogram.domain.login.domain.repository;

import codeshot.photogram.domain.login.domain.entity.LocalLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalLoginRepository extends JpaRepository<LocalLogin, Long> {

    Optional<LocalLogin> findByPhotogramId(String photogramId);
    boolean existsByPhotogramId(String photogramId);
}
