package soon.fridgely.global.support.notification;

public interface NotificationSender {

    /**
     * 사용자에게 알림 메시지 발송
     *
     * @param memberId 수신자 ID
     * @param title    알림 제목
     * @param body  알림 내용
     */
    void send(long memberId, String title, String body);

}