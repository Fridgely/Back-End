package soon.fridgely.global.infra.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import soon.fridgely.global.support.notification.NotificationSender;

@Slf4j
@Profile({"local", "test", "performance"})
@Component
public class LogNotificationSender implements NotificationSender {

    @Override
    public void send(long memberId, String title, String body) {
        log.info("[Notification] Send to MemberId={}", memberId);
        log.info(" - Title: {}", title);
        log.info(" - Body : {}", body);
    }

}