package codeshot.photogram.domain.shot.application;

import java.util.List;

public interface ShotService {

    boolean likePost(Long memberID, Long postId);

    void unlikePost(Long memberID, Long postId);

    long getLikeCount(Long postId);

    List<String> getLikedUsernamesByPostId(Long postId);
}
