package codeshot.photogram.domain.login.application;

import codeshot.photogram.domain.login.domain.LoginType;
import codeshot.photogram.domain.login.domain.entity.LocalLogin;
import codeshot.photogram.domain.login.domain.entity.MemberTerm;
import codeshot.photogram.domain.login.domain.entity.Term;
import codeshot.photogram.domain.login.dto.LocalSignUpRequest;
import codeshot.photogram.domain.login.domain.repository.LocalLoginRepository;
import codeshot.photogram.domain.login.domain.repository.MemberTermRepository;
import codeshot.photogram.domain.login.domain.repository.TermRepository;
import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.domain.member.domain.repository.MemberRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LocalSignUpServiceImpl implements LocalSignUpService {

    private final MemberRepository memberRepository;
    private final LocalLoginRepository localLoginRepository;
    private final TermRepository termRepository;
    private final MemberTermRepository memberTermRepository;
    private final PasswordEncoder passwordEncoder;
    private final AmazonS3 amazonS3;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public void signUp(LocalSignUpRequest request, MultipartFile profileImage) {
        validateDuplicatePhotogramId(request.getPhotogramId());
        validateDuplicateNickName(request.getNickName());

        //S3에 이미지 파일 저장 후 url 가져오기
        String profileImageUrl = uploadFile(profileImage);

        Member member = Member.builder()
                .nickName(request.getNickName())
                .name(request.getName())
                .profileImageUrl(profileImageUrl) // getter사용하지 않고 uploadFile의 리턴값 사용
                .backgroundImageUrl(request.getBackgroundImageUrl())
                .loginType(LoginType.LOCAL)
                .introIndex(request.getIntroIndex())
                .visibility(request.getVisibility())
                .build();

        member.getRoles().add("ROLE_USER");

        memberRepository.save(member);

        LocalLogin login = LocalLogin.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .photogramId(request.getPhotogramId())
                .member(member)
                .build();

        localLoginRepository.save(login);

        List<Term> terms = termRepository.findAllById(request.getAgreedTermIds());
        for (Term term : terms) {
            memberTermRepository.save(MemberTerm.builder()
                    .member(member)
                    .term(term)
                    .build());
        }
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

    private void validateDuplicatePhotogramId(String photogramId) {
        if (localLoginRepository.existsByPhotogramId(photogramId)) {
            throw new IllegalArgumentException("이미 사용 중인 포토그램 ID입니다.");
        }
    }

    private void validateDuplicateNickName(String nickName) {
        if (memberRepository.existsByNickName(nickName)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
    }


}
