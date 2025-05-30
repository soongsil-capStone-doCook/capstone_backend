package codeshot.photogram.domain.member.application;

import codeshot.photogram.domain.login.domain.LoginType;
import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.member.domain.repository.MemberRepository;
import codeshot.photogram.domain.member.dto.MemberResponse;
import codeshot.photogram.domain.member.dto.MemberSearchResponse;
import codeshot.photogram.domain.member.dto.MemberUpdateRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final AmazonS3 amazonS3;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("ID not found: " + memberId));

        //locallogin에서 email 들고오기
        String email = null;
        if (member.getLocalLogin() != null) {
            email = member.getLocalLogin().getEmail();
        }

        return MemberResponse.builder()
                .nickName(member.getNickName())
                .name(member.getName())
                .profileImageUrl(member.getProfileImageUrl())
                .backgroundImageUrl(member.getBackgroundImageUrl())
                .introIndex(member.getIntroIndex())
                .visibility(member.getVisibility())
                .email(email)
                .build();
    }

    @Override
    public void updateMember(Long memberId, MemberUpdateRequest request, MultipartFile profileImage) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("ID not found: " + memberId));
        if (profileImage != null && !profileImage.isEmpty()) {
            String profileImageUrl = uploadFile(profileImage);
            member.updateProfileImageUrl(profileImageUrl);
        }
        member.update(request);
    }

    @Override
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("ID not found: " + memberId));

        // 로그인 타입 로그
        if (member.getLoginType() == LoginType.LOCAL) {
            log.info("[회원 삭제] Local 로그인 회원입니다. memberId={}", memberId);
        } else if (member.getLoginType() == LoginType.SOCIAL) {
            log.info("[회원 삭제] Social 로그인 회원입니다. memberId={}", memberId);
        }

        // LAZY 관계 초기화 (무결성 위반 방지)
        if (member.getLocalLogin() != null) member.getLocalLogin().getId();
        if (member.getSocialLogin() != null) member.getSocialLogin().getId();
        member.getMemberTerms().size(); // List 초기화
        member.getRoles().size();       // @ElementCollection 초기화

        // 삭제 로그
        log.info("[회원 삭제 시작] memberId={}, roles={}, terms={}, local={}, social={}",
                memberId,
                member.getRoles(),
                member.getMemberTerms().size(),
                member.getLocalLogin() != null,
                member.getSocialLogin() != null
        );

        memberRepository.delete(member);

        // 완료 로그
        log.info("[회원 삭제 완료] memberId={}", memberId);
    }


    @Override
    public List<MemberSearchResponse> findMembersByNickname(String nickname) {
        return memberRepository.findByNickNameStartingWith(nickname)
                .stream()
                .map(member -> MemberSearchResponse.builder()
                    .memberId(member.getMemberID())
                    .nickName(member.getNickName())
                    .name(member.getName())
                    .profileImageUrl(member.getProfileImageUrl())
                    .build())
                .collect(Collectors.toList());
    }

    @Override
    public String uploadFile(MultipartFile file) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            // S3에 파일 업로드
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);

            // Public URL 생성
            return amazonS3.getUrl(bucket, fileName).toString();
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }
}
