package soon.fridgely.global.infra.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static soon.fridgely.global.support.fixture.MemberDeviceFixture.memberDevice;
import static soon.fridgely.global.support.fixture.MemberFixture.member;

@ExtendWith(MockitoExtension.class)
class FcmNotificationSenderUnitTest {

    @InjectMocks
    private FcmNotificationSender fcmNotificationSender;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private MemberDeviceRepository memberDeviceRepository;

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    private Member member;
    private MemberDevice memberDevice;

    @BeforeEach
    void setUp() {
        this.member = member(fixtureMonkey)
            .set("id", 1L)
            .sample();
        this.memberDevice = memberDevice(fixtureMonkey, member)
            .sample();
    }

    @Test
    void 회원의_모든_디바이스에_알림을_전송한다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);
        MemberDevice device2 = memberDevice(fixtureMonkey, member).sample();

        given(memberDeviceRepository.findAllByMemberId(member.getId())).willReturn(List.of(memberDevice, device2));
        given(firebaseMessaging.send(any(Message.class))).willReturn("messageId");

        // when
        fcmNotificationSender.send(member.getId(), title, body);

        // then
        then(firebaseMessaging).should(times(2)).send(any(Message.class));
    }

    @Test
    void 등록된_디바이스가_없다면_실행되지_않는다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);
        var memberId = 1L;

        given(memberDeviceRepository.findAllByMemberId(memberId)).willReturn(Collections.emptyList());

        // when
        fcmNotificationSender.send(memberId, title, body);

        // then
        then(firebaseMessaging).should(never()).send(any(Message.class));
    }

    @Test
    void target_screen데이터가_FOOD_STATUS로_포함된다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);

        given(memberDeviceRepository.findAllByMemberId(member.getId())).willReturn(List.of(memberDevice));
        given(firebaseMessaging.send(any(Message.class))).willReturn("messageId");

        // when
        fcmNotificationSender.send(member.getId(), title, body);

        // then
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        then(firebaseMessaging).should().send(messageCaptor.capture());
        assertThat(messageCaptor.getValue()).isNotNull();
    }

    @Test
    void 한개의_알림_전송에_실패해도_나머지_알림은_전송된다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);
        var device2 = memberDevice(fixtureMonkey, member).sample();

        given(memberDeviceRepository.findAllByMemberId(member.getId())).willReturn(List.of(memberDevice, device2));
        given(firebaseMessaging.send(any(Message.class)))
            .willThrow(mock(FirebaseMessagingException.class))
            .willReturn("messageId");

        // when
        fcmNotificationSender.send(member.getId(), title, body);

        // then
        then(firebaseMessaging).should(times(2)).send(any(Message.class));
    }

    @Test
    void 하나의_디바이스에_알림을_전송한다() throws FirebaseMessagingException {
        // given
        var title = fixtureMonkey.giveMeOne(String.class);
        var body = fixtureMonkey.giveMeOne(String.class);

        given(memberDeviceRepository.findAllByMemberId(member.getId())).willReturn(List.of(memberDevice));
        given(firebaseMessaging.send(any(Message.class))).willReturn("messageId");

        // when
        fcmNotificationSender.send(member.getId(), title, body);

        // then
        then(firebaseMessaging).should(times(1)).send(any(Message.class));
    }

}