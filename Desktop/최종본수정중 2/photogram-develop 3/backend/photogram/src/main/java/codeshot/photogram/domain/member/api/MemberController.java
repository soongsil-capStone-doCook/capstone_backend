package codeshot.photogram.domain.member.api;

import codeshot.photogram.domain.login.dto.LocalSignUpRequest;
import codeshot.photogram.domain.member.application.MemberService;
import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.member.domain.repository.MemberRepository;
import codeshot.photogram.domain.member.dto.MemberResponse;
import codeshot.photogram.domain.member.dto.MemberSearchResponse;
import codeshot.photogram.domain.member.dto.MemberUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;


    //Jwt토큰에서 파싱된 사용자 정보 조회
    @GetMapping("/me")
    public MemberResponse getMember(@AuthenticationPrincipal(expression = "username") String memberIdStr) {
        Long memberId = Long.valueOf(memberIdStr);
        return memberService.getMember(memberId);
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> update(@AuthenticationPrincipal(expression = "username") String memberIdStr,
                                       @RequestPart("updateRequest") MemberUpdateRequest request,
                                       @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        Long memberId = Long.valueOf(memberIdStr);
        memberService.updateMember(memberId, request, profileImage);
        return ResponseEntity.ok().build();
    }

    //회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMember(@AuthenticationPrincipal(expression = "username") String memberIdStr) {
        Long memberId = Long.valueOf(memberIdStr);
        memberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<MemberSearchResponse>> searchMembers(@RequestParam String nickname) {
        List<MemberSearchResponse> result = memberService.findMembersByNickname(nickname);
        return ResponseEntity.ok(result);
    }


}
