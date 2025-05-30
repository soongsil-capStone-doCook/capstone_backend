//package codeshot.photogram.domain.login.application;
//
//import codeshot.photogram.domain.login.domain.entity.Term;
//import codeshot.photogram.domain.login.domain.repository.LocalLoginRepository;
//import codeshot.photogram.domain.login.domain.repository.MemberTermRepository;
//import codeshot.photogram.domain.login.domain.repository.TermRepository;
//import codeshot.photogram.domain.login.dto.LocalSignUpRequest;
//import codeshot.photogram.domain.member.domain.Visibility;
//import codeshot.photogram.domain.member.domain.repository.MemberRepository;
//import codeshot.photogram.global.security.jwt.JwtTokenProvider;
//import codeshot.photogram.global.security.jwt.dto.JwtToken;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.lang.reflect.Field;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.*;
//
//class LocalSignUpServiceImplTest {
//
//    private MemberRepository memberRepository;
//    private LocalLoginRepository localLoginRepository;
//    private TermRepository termRepository;
//    private MemberTermRepository memberTermRepository;
//    private PasswordEncoder passwordEncoder;
//    private JwtTokenProvider jwtTokenProvider;
//    private LocalSignUpServiceImpl signUpService;
//
//    @BeforeEach
//    void setUp() {
//        memberRepository = mock(MemberRepository.class); // cascade 구조라 사용 안함
//        localLoginRepository = mock(LocalLoginRepository.class);
//        termRepository = mock(TermRepository.class);
//        memberTermRepository = mock(MemberTermRepository.class);
//        passwordEncoder = mock(PasswordEncoder.class);
//        jwtTokenProvider = mock(JwtTokenProvider.class);
//
//        signUpService = new LocalSignUpServiceImpl(
//                memberRepository,
//                localLoginRepository,
//                termRepository,
//                memberTermRepository,
//                passwordEncoder,
//                jwtTokenProvider
//        );
//    }
//
//    @Test
//    void 회원가입_성공_테스트() throws Exception {
//        // given
//        LocalSignUpRequest request = new LocalSignUpRequest();
//        request.setPhotogramId("testId");
//        request.setPassword("plainPassword");
//        request.setNickName("tester");
//        request.setName("테스터");
//        request.setVisibility(Visibility.PUBLIC);
//        request.setAgreedTermIds(List.of(1L, 2L));
//
//        when(localLoginRepository.existsByPhotogramId("testId")).thenReturn(false);
//        when(memberRepository.existsByNickName("tester")).thenReturn(false);
//        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
//
//        Term term1 = Term.builder().title("term1").optional(false).build();
//        Term term2 = Term.builder().title("term2").optional(false).build();
//        setId(term1, 1L);
//        setId(term2, 2L);
//        when(termRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(term1, term2));
//
//        when(jwtTokenProvider.generateToken(any(), any()))
//                .thenReturn(JwtToken.builder()
//                        .accessToken("access")
//                        .refreshToken("refresh")
//                        .grantType("Bearer")
//                        .build());
//
//        // when
//        String accessToken = signUpService.signUp(request);
//
//        // then
//        assertEquals("access", accessToken);
//        verify(localLoginRepository).save(any());
//        verify(memberTermRepository, times(2)).save(any());
//        verify(jwtTokenProvider).generateToken(any(), any());
//    }
//
//    private void setId(Object entity, Long id) throws Exception {
//        Field idField = entity.getClass().getDeclaredField("id");
//        idField.setAccessible(true);
//        idField.set(entity, id);
//    }
//}
