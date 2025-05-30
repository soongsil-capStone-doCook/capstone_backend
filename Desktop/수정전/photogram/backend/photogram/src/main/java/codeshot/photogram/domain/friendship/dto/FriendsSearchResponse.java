package codeshot.photogram.domain.friendship.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendsSearchResponse {
    private Long memberId;
    private String nickName;
    private String profileImageUrl;
}
