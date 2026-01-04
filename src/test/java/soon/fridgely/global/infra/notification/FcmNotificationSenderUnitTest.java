package soon.fridgely.global.infra.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmNotificationSenderUnitTest {

    @InjectMocks
    private FcmNotificationSender fcmNotificationSender;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private MemberDeviceRepository memberDeviceRepository;

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Test
    void 회원의_모든_디바이스에_알림을_전송한다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);

        Member member = createMember(1L);
        MemberDevice device1 = createMemberDevice(fixtureMonkey.giveMeOne(String.class));
        MemberDevice device2 = createMemberDevice(fixtureMonkey.giveMeOne(String.class));
        given(memberDeviceRepository.findAllByMemberId(1L)).willReturn(List.of(device1, device2));
        given(firebaseMessaging.send(any(Message.class))).willReturn("messageId");

        // when
        fcmNotificationSender.send(member.getId(), title, body);

        // then
        verify(firebaseMessaging, times(2)).send(any(Message.class));
    }

    @Test
    void 등록된_디바이스가_없다면_실행되지_않는다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);
        long memberId = 1L;
        given(memberDeviceRepository.findAllByMemberId(memberId)).willReturn(Collections.emptyList());

        // when
        fcmNotificationSender.send(memberId, title, body);

        // then
        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void target_screen데이터가_FOOD_STATUS로_포함된다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);

        Member member = createMember(1L);
        MemberDevice device = createMemberDevice(fixtureMonkey.giveMeOne(String.class));
        given(memberDeviceRepository.findAllByMemberId(member.getId())).willReturn(List.of(device));
        given(firebaseMessaging.send(any(Message.class))).willReturn("messageId");

        // when
        fcmNotificationSender.send(member.getId(), title, body);

        // then
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue()).isNotNull();
    }

    @Test
    void 한개의_알림_전송에_실패해도_나머지_알림은_전송된다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);

        Member member = createMember(1L);
        MemberDevice device1 = createMemberDeviceWithMember(member, fixtureMonkey.giveMeOne(String.class));
        MemberDevice device2 = createMemberDevice(fixtureMonkey.giveMeOne(String.class));
        given(memberDeviceRepository.findAllByMemberId(member.getId())).willReturn(List.of(device1, device2));
        given(firebaseMessaging.send(any(Message.class)))
            .willThrow(mock(FirebaseMessagingException.class))
            .willReturn("messageId");

        // when
        fcmNotificationSender.send(member.getId(), title, body);

        // then
        verify(firebaseMessaging, times(2)).send(any(Message.class));
    }

    @Test
    void 하나의_디바이스에_알림을_전송한다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);

        Member member = createMember(1L);
        MemberDevice device = createMemberDevice(fixtureMonkey.giveMeOne(String.class));
        given(memberDeviceRepository.findAllByMemberId(member.getId())).willReturn(List.of(device));
        given(firebaseMessaging.send(any(Message.class))).willReturn("messageId");

        // when
        fcmNotificationSender.send(member.getId(), title, body);

        // then
        verify(firebaseMessaging, times(1)).send(any(Message.class));
    }

    private Member createMember(long id) {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(id);
        return member;
    }

    private MemberDevice createMemberDevice(String token) {
        MemberDevice device = mock(MemberDevice.class);
        given(device.getToken()).willReturn(token);
        return device;
    }

    private MemberDevice createMemberDeviceWithMember(Member member, String token) {
        MemberDevice device = createMemberDevice(token);
        given(device.getMember()).willReturn(member);
        return device;
    }

}