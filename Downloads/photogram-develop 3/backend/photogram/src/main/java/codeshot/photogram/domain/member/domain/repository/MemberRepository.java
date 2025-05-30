package codeshot.photogram.domain.member.domain.repository;

import codeshot.photogram.domain.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 닉네임 중복 확인
    boolean existsByNickName(String nickName);

    // ID(PK)로 사용자 조회
    Optional<Member> findByMemberID(Long memberID);

    //전체 멤버에서 검색
    List<Member> findByNickNameStartingWith(String prefix);

    Optional<Member> findByNickName(String nickname);

}

