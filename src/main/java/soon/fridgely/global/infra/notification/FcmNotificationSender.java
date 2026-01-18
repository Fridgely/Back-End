package soon.fridgely.global.infra.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;
import soon.fridgely.global.support.notification.NotificationSender;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Profile("live")
@Component
public class FcmNotificationSender implements NotificationSender {

    private static final String TARGET_SCREEN_KEY = "target_screen";
    private static final String TARGET_SCREEN = "FOOD_STATUS";

    private final FirebaseMessaging firebaseMessaging;
    private final MemberDeviceRepository memberDeviceRepository;

    @Override
    public void send(long memberId, String title, String body) {
        List<MemberDevice> devices = memberDeviceRepository.findAllByMemberId(memberId);
        if (devices.isEmpty()) {
            log.debug("[FcmSender] 등록된 디바이스가 없어 알림을 전송하지 않음. (MemberId={})", memberId);
            return;
        }

        devices.forEach(device -> sendToDevice(device, title, body));
    }

    private void sendToDevice(MemberDevice device, String title, String body) {
        try {
            Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

            Message message = Message.builder()
                .setToken(device.getToken())
                .setNotification(notification)
                .putData(TARGET_SCREEN_KEY, TARGET_SCREEN)
                .build();

            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            log.error("[FcmSender] 알림 전송 실패 (MemberId={}, Token={})", device.getMember().getId(), device.getToken(), e);
        }
    }

}