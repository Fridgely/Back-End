package soon.fridgely.global.infra.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import soon.fridgely.global.support.notification.NotificationSender;

@Slf4j
@Primary
@Component
public class LogNotificationSender implements NotificationSender {

    @Override
    public void send(long memberId, String title, String message) {
        log.info("[Notification] Send to MemberId={}", memberId);
        log.info(" - Title: {}", title);
        log.info(" - Body : {}", message);
    }

}