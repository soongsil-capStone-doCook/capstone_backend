package codeshot.photogram.domain.shot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LikeRequestDTO {

    private Long memberID;
    private Long postId;

    // 기본 생성자
    public LikeRequestDTO() {}

    public LikeRequestDTO(Long postId) {
        this.postId = postId;
    }

    public Long getMemberID() {
        return memberID;
    }

    public void setMemberID(Long memberID) {
        this.memberID = memberID;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }
}
