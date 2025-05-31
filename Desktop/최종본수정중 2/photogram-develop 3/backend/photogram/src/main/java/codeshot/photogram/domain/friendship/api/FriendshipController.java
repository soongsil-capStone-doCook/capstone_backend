package codeshot.photogram.domain.friendship.api;

import codeshot.photogram.domain.friendship.application.FriendshipService;
import codeshot.photogram.domain.friendship.dto.FriendsSearchResponse;
import codeshot.photogram.domain.friendship.dto.FriendshipRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<Void> friendRequest(@AuthenticationPrincipal(expression = "username") String memberIdStr, @RequestBody FriendshipRequest request) {
        Long requesterId = Long.valueOf(memberIdStr);
        friendshipService.sendFriendRequest(requesterId, request.getReceiverId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<FriendsSearchResponse>> searchFriends(
            @AuthenticationPrincipal(expression = "username") String memberIdStr,
            @RequestParam boolean accepted) {

        Long memberId = Long.valueOf(memberIdStr);
        List<FriendsSearchResponse> friends = accepted
                ? friendshipService.getFriends(memberId)
                : friendshipService.getReceivedRequests(memberId);

        return ResponseEntity.ok(friends);
    }


    @DeleteMapping
    public ResponseEntity<Void> deleteFriendByNickname(
            @AuthenticationPrincipal(expression = "username") String memberIdStr,
            @RequestParam String nickname) {
        Long myId = Long.valueOf(memberIdStr);
        friendshipService.deleteFriendByNickname(myId, nickname);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/request")
    public ResponseEntity<Void> acceptRequest(
            @AuthenticationPrincipal(expression = "username") String myIdStr,
            @RequestParam String nickName) {

        Long myId = Long.valueOf(myIdStr);
        friendshipService.acceptRequest(myId, nickName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/request")
    public ResponseEntity<Void> cancelRequest(
            @AuthenticationPrincipal(expression = "username") String memberIdStr,
            @RequestParam String nickName) {

        Long myId = Long.valueOf(memberIdStr);
        friendshipService.cancelRequest(myId, nickName);
        return ResponseEntity.noContent().build();
    }


}
