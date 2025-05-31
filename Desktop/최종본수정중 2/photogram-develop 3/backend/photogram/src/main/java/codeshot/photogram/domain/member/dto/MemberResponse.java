package codeshot.photogram.domain.member.dto;

import codeshot.photogram.domain.member.domain.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class MemberResponse {
    private String nickName;

    private String name;

    private String profileImageUrl;

    private String backgroundImageUrl;

    private String introIndex;

    private Visibility visibility;

    private String email;

}
