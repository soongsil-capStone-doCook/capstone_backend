package codeshot.photogram.domain.member.application;

import codeshot.photogram.domain.member.dto.MemberResponse;
import codeshot.photogram.domain.member.dto.MemberSearchResponse;
import codeshot.photogram.domain.member.dto.MemberUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MemberService {
    MemberResponse getMember(Long memberId);

    void updateMember(Long memberId, MemberUpdateRequest request , MultipartFile profileImage);

    void deleteMember(Long memberId);

    List<MemberSearchResponse> findMembersByNickname(String nickname);

    public String uploadFile(MultipartFile file);

}

