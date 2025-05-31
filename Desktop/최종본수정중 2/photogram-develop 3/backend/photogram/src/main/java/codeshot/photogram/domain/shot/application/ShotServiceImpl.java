package codeshot.photogram.domain.shot.application;

import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.member.domain.repository.MemberRepository;
import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.post.domain.repository.PostRepository;
import codeshot.photogram.domain.shot.domain.entity.Shot;
import codeshot.photogram.domain.shot.domain.repository.ShotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // ✅ 클래스 레벨에 읽기 전용 트랜잭션 기본 설정
public class ShotServiceImpl implements ShotService {

    private final ShotRepository shotRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    public ShotServiceImpl(ShotRepository shotRepository, MemberRepository memberRepository, PostRepository postRepository) {
        this.shotRepository = shotRepository;
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
    }

    @Override
    @Transactional // ✅ 쓰기 작업은 @Transactional 필요
    public boolean likePost(Long memberID, Long postId) {
        Member member = memberRepository.findByMemberID(memberID)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + memberID));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));

        Shot shot = new Shot();
        if (!shotRepository.existsByMemberAndPost(member, post)) {
            shot.setMember(member);
            shot.setPost(post);
            shot.setLiked(true);
            shotRepository.save(shot);
        } else {
            throw new RuntimeException("User " + memberID + " has already liked post " + postId + ".");
        }
        return shot.isLiked();
    }

    @Override
    @Transactional // ✅ 쓰기 작업은 @Transactional 필요
    public void unlikePost(Long memberID, Long postId) {
        Member member = memberRepository.findByMemberID(memberID)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + memberID));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));

        shotRepository.findByMemberAndPost(member, post)
                .ifPresentOrElse(
                        shotRepository::delete,
                        () -> { throw new RuntimeException("Like record not found for user " + memberID + " and post " + postId + "."); }
                );
    }

    @Override
    public long getLikeCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));
        return shotRepository.countByPost(post);
    }

    @Override
    public List<String> getLikedUsernamesByPostId(Long postId) {
        // Post 존재 여부 확인 (옵션: 없는 게시물에 대한 조회 시도 방지)
        postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));

        List<Shot> likes = shotRepository.findByPostIdWithMember(postId);

        return likes.stream()
                .map(shot -> shot.getMember().getNickName())
                .collect(Collectors.toList());
    }
}