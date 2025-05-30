package codeshot.photogram.domain.friendship.application;

import codeshot.photogram.domain.friendship.domain.entity.Friendship;
import codeshot.photogram.domain.friendship.domain.repository.FriendshipRepository;
import codeshot.photogram.domain.friendship.dto.FriendsSearchResponse;
import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final MemberRepository memberRepository;

    public void sendFriendRequest(Long requesterId, Long receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }

        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new NoSuchElementException("요청자를 찾을 수 없습니다."));
        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new NoSuchElementException("수신자를 찾을 수 없습니다."));

        // 중복 요청 또는 이미 친구인지 확인
        boolean exists = friendshipRepository.findByMembers(requester, receiver).isPresent();
        if (exists) {
            throw new IllegalStateException("이미 친구 관계이거나 요청이 존재합니다.");
        }

        Friendship request = Friendship.builder()
                .requester(requester)
                .receiver(receiver)
                .accepted(false)
                .build();

        friendshipRepository.save(request);
    }

    @Override
    public List<FriendsSearchResponse> getFriends(Long memberId) {
        Member me = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));

        return friendshipRepository.findAllFriends(me).stream()
                .map(f -> {
                    Member friend = f.getRequester().equals(me) ? f.getReceiver() : f.getRequester(); //내가 requester인지 receiver인지 구분하여 friend에 친구 할당
                    return FriendsSearchResponse.builder()
                            .memberId(friend.getMemberID())
                            .nickName(friend.getNickName())
                            .profileImageUrl(friend.getProfileImageUrl())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFriendByNickname(Long myId, String nickname) {
        Member me = memberRepository.findById(myId)
                .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));

        Member target = memberRepository.findByNickName(nickname)
                .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));

        Friendship friendship = friendshipRepository.findByMembers(me, target)
                .orElseThrow(() -> new NoSuchElementException("친구 관계가 존재하지 않습니다."));

        friendshipRepository.delete(friendship);
    }

    @Override
    public List<FriendsSearchResponse> getReceivedRequests(Long memberId) {
        Member me = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));

        return friendshipRepository.findAllByReceiverAndAcceptedFalse(me)
                .stream()
                .map(f -> {
                    Member requester = f.getRequester();
                    return FriendsSearchResponse.builder()
                            .memberId(requester.getMemberID())
                            .nickName(requester.getNickName())
                            .profileImageUrl(requester.getProfileImageUrl())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void acceptRequest(Long myId, String nickName) {
        Member me = memberRepository.findById(myId)
                .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));

        Member target = memberRepository.findByNickName(nickName)
                .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));


        Friendship friendship = friendshipRepository.findByRequesterAndReceiver(target, me)
                .orElseThrow(() -> new IllegalArgumentException("요청 없음"));

        friendship.accept(); // accepted = true
    }

    @Override
    public void cancelRequest(Long myId, String nickName) {
        Member me = memberRepository.findById(myId)
                .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));

        Member target = memberRepository.findByNickName(nickName)
                .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));

        Friendship friendship = friendshipRepository.findByMembers(me, target)
                .orElseThrow(() -> new IllegalArgumentException("요청 없음"));

        friendshipRepository.delete(friendship);
    }


}
