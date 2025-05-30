package codeshot.photogram.domain.login.domain.repository;

import codeshot.photogram.domain.login.domain.entity.MemberTerm;
import codeshot.photogram.domain.login.domain.entity.MemberTermId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTermRepository extends JpaRepository<MemberTerm, MemberTermId> {
}
