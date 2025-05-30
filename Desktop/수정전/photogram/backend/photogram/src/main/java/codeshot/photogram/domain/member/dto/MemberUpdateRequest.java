package codeshot.photogram.domain.member.dto;

import codeshot.photogram.domain.member.domain.Visibility;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateRequest {
    private String nickName;

    private String introIndex;

    private Visibility visibility;
}
