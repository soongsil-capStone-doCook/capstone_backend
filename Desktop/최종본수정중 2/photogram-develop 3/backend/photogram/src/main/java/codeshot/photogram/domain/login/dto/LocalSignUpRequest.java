package codeshot.photogram.domain.login.dto;

import codeshot.photogram.domain.member.domain.Visibility;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LocalSignUpRequest {

    private String nickName;

    private String name;

    private String profileImageUrl;

    private String backgroundImageUrl;

    private String introIndex;

    private Visibility visibility;

    private String email;

    private String password;

    private String photogramId;

    private List<Long> agreedTermIds;
}
