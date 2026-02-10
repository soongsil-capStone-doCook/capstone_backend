package capstone.fridge.domain.notification.service;

import capstone.fridge.domain.member.domain.entity.Member;
import capstone.fridge.domain.member.domain.repository.MemberRepository;
import capstone.fridge.global.error.code.status.ErrorStatus;
import capstone.fridge.domain.member.exception.memberException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public void sendPush(Long memberId, String title, String body) {
        // 1. 회원 조회 및 FCM 토큰 획득
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new memberException(ErrorStatus._BAD_REQUEST));

        String token = member.getFcmToken();

        // 토큰이 없는 경우 (로그인 안 함, 알림 거부 등) 발송 스킵
        if (token == null || token.isEmpty()) {
            log.warn("[FCM] 토큰이 존재하지 않아 알림 발송 실패. MemberID={}", memberId);
            return;
        }

        // 2. 메시지 구성
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                // 필요하다면 추가 데이터 전송 가능
                // .putData("route", "/recipe/123")
                .build();

        // 3. 발송
        try {
            String response = firebaseMessaging.send(message);
            log.info("[FCM] 알림 발송 성공. MemberID={}, Response={}", memberId, response);
        } catch (Exception e) {
            log.error("[FCM] 알림 발송 실패. MemberID={}", memberId, e);
            // 필요 시 예외 처리 또는 재시도 로직 추가
        }
    }
}