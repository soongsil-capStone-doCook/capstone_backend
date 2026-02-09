package capstone.fridge.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    // 실제로는 여기에 FCM Client 등이 주입되어야 합니다.
    // private final FirebaseMessaging firebaseMessaging;

    public void sendPush(Long memberId, String title, String body) {
        // 실제 구현 시: Member 엔티티에서 fcmToken을 가져와서 send
        log.info("==================================================");
        log.info("[PUSH 발송] To: MemberID={}", memberId);
        log.info("[Title] {}", title);
        log.info("[Body] {}", body);
        log.info("==================================================");

        /* // FCM 예시 코드
        Message message = Message.builder()
            .setToken(userFcmToken)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .build();
        firebaseMessaging.send(message);
        */
    }
}