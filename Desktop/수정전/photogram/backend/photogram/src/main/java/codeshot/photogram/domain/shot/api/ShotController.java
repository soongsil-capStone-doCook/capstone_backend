package codeshot.photogram.domain.shot.api;

import codeshot.photogram.domain.shot.application.ShotService;
import codeshot.photogram.domain.shot.dto.LikeCountResponseDTO;
import codeshot.photogram.domain.shot.dto.LikeRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/histories")
public class ShotController {

    private final ShotService shotService;

    public ShotController(ShotService shotService) {
        this.shotService = shotService;
    }

    // 게시물에 좋아요
    @PostMapping("/like")
    public boolean likePost(@AuthenticationPrincipal(expression = "username") String memberIdStr,
                            @RequestBody LikeRequestDTO postId) {
        Long memberId = Long.valueOf(memberIdStr);
        return shotService.likePost(memberId, postId.getPostId());
    }

    // 게시물에 좋아요 취소
    @PostMapping("/unlike")
    public ResponseEntity<String> unlikePost(@AuthenticationPrincipal(expression = "username") String memberIdStr,
                                             @RequestBody LikeRequestDTO postId) {
        Long memberId = Long.valueOf(memberIdStr);
        try {
            shotService.unlikePost(memberId, postId.getPostId());
            return ResponseEntity.ok("Post unliked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // 게시물의 좋아요 수 조회
    @GetMapping("/like-count/{postId}")
    public ResponseEntity<LikeCountResponseDTO> getLikeCount(@PathVariable Long postId) {
        try {
            long likeCount = shotService.getLikeCount(postId);
            return ResponseEntity.ok(new LikeCountResponseDTO(likeCount));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    @GetMapping("/{postId}/like")
    public ResponseEntity<List<String>> getLikedUsersForPost(@PathVariable Long postId) {
        try {
            List<String> likedUsernames = shotService.getLikedUsernamesByPostId(postId);
            if (likedUsernames.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(likedUsernames);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
