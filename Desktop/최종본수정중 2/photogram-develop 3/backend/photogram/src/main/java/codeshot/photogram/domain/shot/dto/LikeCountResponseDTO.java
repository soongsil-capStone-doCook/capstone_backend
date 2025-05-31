package codeshot.photogram.domain.shot.dto;

public class LikeCountResponseDTO {

    private long likeCount;

    public LikeCountResponseDTO(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }
}
