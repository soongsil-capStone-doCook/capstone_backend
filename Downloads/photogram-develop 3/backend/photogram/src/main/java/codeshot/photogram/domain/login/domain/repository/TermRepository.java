package codeshot.photogram.domain.login.domain.repository;

import codeshot.photogram.domain.login.domain.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {
}
