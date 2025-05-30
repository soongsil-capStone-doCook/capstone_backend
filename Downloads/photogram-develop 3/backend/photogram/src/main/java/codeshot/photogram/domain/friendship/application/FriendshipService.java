package codeshot.photogram.domain.friendship.application;

import codeshot.photogram.domain.friendship.dto.FriendsSearchResponse;

import java.util.List;

public interface FriendshipService {

    void sendFriendRequest(Long requesterId, Long receiverId);

    List<FriendsSearchResponse> getFriends(Long memberId);

    void deleteFriendByNickname(Long myId, String nickname);

    List<FriendsSearchResponse> getReceivedRequests(Long memberId);

    void acceptRequest(Long myId, String nickname);

    void cancelRequest(Long myId, String nickname);
}
