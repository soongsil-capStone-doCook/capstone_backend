package codeshot.photogram.domain.friendship.domain.repository;

import codeshot.photogram.domain.friendship.domain.entity.Friendship;
import codeshot.photogram.domain.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Modifying
    @Query("DELETE FROM Friendship f WHERE " +
            "(f.requester.memberID = :id1 AND f.receiver.memberID = :id2) OR " +
            "(f.requester.memberID = :id2 AND f.receiver.memberID = :id1)")
    void deleteByMemberIds(@Param("id1") Long id1, @Param("id2") Long id2);

    // 이미 요청한 적이 있는지 확인
    Optional<Friendship> findByRequesterAndReceiver(Member requester, Member receiver);

    // 내가 보낸 요청 중 수락되지 않은 목록
    List<Friendship> findAllByRequesterAndAcceptedFalse(Member requester);

    // 나에게 온 요청 중 수락되지 않은 목록
    List<Friendship> findAllByReceiverAndAcceptedFalse(Member receiver);

    // 수락된 친구 목록 (내가 요청했든, 받았든)
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :member OR f.receiver = :member) AND f.accepted = true")
    List<Friendship> findAllFriends(@Param("member") Member member);

    // 특정 두 명 사이의 관계를 찾기 (방향 무시하고)
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester = :member1 AND f.receiver = :member2) OR " +
            "(f.requester = :member2 AND f.receiver = :member1)")
    Optional<Friendship> findByMembers(@Param("member1") Member m1, @Param("member2") Member m2);

}
