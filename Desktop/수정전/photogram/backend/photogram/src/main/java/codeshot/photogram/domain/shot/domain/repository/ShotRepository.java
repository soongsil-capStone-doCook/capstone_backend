package codeshot.photogram.domain.shot.domain.repository;

import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.shot.domain.entity.Shot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShotRepository extends JpaRepository<Shot, Long> {

    // ✅ 기존에 잘 동작하던 메서드 1: 좋아요 눌렀는지 여부 확인 (Member 객체와 Post 객체로 조회)
    boolean existsByMemberAndPost(Member member, Post post);

    // ✅ 기존에 잘 동작하던 메서드 2: 특정 좋아요 기록 찾기 (Member 객체와 Post 객체로 조회)
    Optional<Shot> findByMemberAndPost(Member member, Post post);

    // ✅ 기존에 잘 동작하던 메서드 3: 게시물의 좋아요 수 조회 (Post 객체로 조회)
    long countByPost(Post post);

    // ✅ 새로 추가된 엔드포인트(getLikedUsernamesByPostId)를 위한 쿼리
    @Query("SELECT s FROM Shot s JOIN FETCH s.member WHERE s.post.id = :postId")
    List<Shot> findByPostIdWithMember(@Param("postId") Long postId);

}