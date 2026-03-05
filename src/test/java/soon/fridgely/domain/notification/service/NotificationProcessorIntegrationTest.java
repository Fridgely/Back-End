package soon.fridgely.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.notification.NotificationSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.NotificationSettingFixture.notificationSetting;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class NotificationProcessorIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private NotificationProcessor notificationProcessor;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @MockitoBean
    private NotificationSender notificationSender;

    private Member member;
    private Refrigerator refrigerator;
    private Category category;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(
            member(fixtureMonkey).sample()
        );
        this.refrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );
        memberRefrigeratorRepository.save(
            MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER)
        );
        this.category = categoryRepository.save(
            category(fixtureMonkey, refrigerator, member).sample()
        );
    }

    @Test
    void 만료_예정_음식이_있으면_알림을_발송한다() {
        // given
        foodRepository.save(
            food(fixtureMonkey, refrigerator, member, category)
                .set("expirationDate", LocalDateTime.now().plusDays(3L))
                .sample()
        );

        var setting = notificationSetting(fixtureMonkey, member)
            .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 3))
            .sample();
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.processExpiration(member.getId());

        // then
        verify(notificationSender, timeout(2000))
            .send(eq(member.getId()), anyString(), anyString());
    }

    @Test
    void 만료_예정_음식이_없으면_알림을_발송하지_않는다() throws InterruptedException {
        // given
        notificationSettingRepository.save(notificationSetting(fixtureMonkey, member)
            .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 3))
            .sample()
        );

        // when
        notificationProcessor.processExpiration(member.getId());

        // then
        Thread.sleep(500);
        verify(notificationSender, never())
            .send(anyLong(), anyString(), anyString());
    }

    @Test
    void 알림_스케줄의_일수에_따라_정확한_날짜의_음식을_조회한다() {
        // given
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "사과")
                .set("expirationDate", LocalDateTime.now().plusDays(7L))
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "바나나")
                .set("expirationDate", LocalDateTime.now().plusDays(3L))
                .sample()
        ));

        notificationSettingRepository.save(notificationSetting(fixtureMonkey, member)
            .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 7))
            .sample());

        // when
        notificationProcessor.processExpiration(member.getId());

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationSender, timeout(2000))
            .send(eq(member.getId()), anyString(), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("사과");
    }

    @Test
    void 여러_음식이_만료_예정일_때_모든_음식을_포함한_알림을_발송한다() {
        // given
        LocalDateTime expiryDateTime = LocalDateTime.now().plusDays(1L);
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category)
                .set("expirationDate", expiryDateTime)
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("expirationDate", expiryDateTime)
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("expirationDate", expiryDateTime)
                .sample()
        ));

        var setting = notificationSetting(fixtureMonkey, member)
            .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 1))
            .sample();
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.processExpiration(member.getId());

        // then
        verify(notificationSender, timeout(2000))
            .send(eq(member.getId()), anyString(), contains("외 2개"));
    }

    @Test
    void 다른_회원의_음식은_알림에_포함되지_않는다() {
        // given
        Member otherMember = memberRepository.save(
            member(fixtureMonkey).sample()
        );

        Refrigerator otherRefrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );

        Category otherCategory = categoryRepository.save(
            category(fixtureMonkey, otherRefrigerator, otherMember).sample()
        );

        LocalDateTime expiryDateTime = LocalDateTime.now().plusDays(3L);
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category)
                .set("expirationDate", expiryDateTime)
                .sample(),
            food(fixtureMonkey, otherRefrigerator, otherMember, otherCategory)
                .set("expirationDate", expiryDateTime)
                .sample()
        ));

        var setting = notificationSetting(fixtureMonkey, member)
            .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 3))
            .sample();
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.processExpiration(member.getId());

        // then
        verify(notificationSender, timeout(2000))
            .send(eq(member.getId()), anyString(), anyString());
        verify(notificationSender, never())
            .send(eq(otherMember.getId()), anyString(), anyString());
    }

    @Test
    void 정확히_N일_후_만료되는_음식만_조회한다() {
        // given
        foodRepository.saveAll(List.of(
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "우유")
                .set("expirationDate", LocalDateTime.now().plusDays(3L))
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "계란")
                .set("expirationDate", LocalDateTime.now().plusDays(2L))
                .sample(),
            food(fixtureMonkey, refrigerator, member, category)
                .set("name", "치즈")
                .set("expirationDate", LocalDateTime.now().plusDays(4L))
                .sample()
        ));

        var setting = notificationSetting(fixtureMonkey, member)
            .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 3))
            .sample();
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.processExpiration(member.getId());

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationSender, timeout(2000))
            .send(eq(member.getId()), anyString(), messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).contains("우유");
        assertThat(sentMessage).doesNotContain("외");
    }

    @Test
    void 재고_소진_알림이_켜져있으면_알림을_발송한다() {
        // given
        foodRepository.save(food(fixtureMonkey, refrigerator, member, category)
            .set("name", "우유")
            .set("expirationDate", LocalDateTime.now())
            .set("quantity", Quantity.register(BigDecimal.ZERO, Unit.G))
            .sample());

        notificationSettingRepository.save(
            notificationSetting(fixtureMonkey, member)
                .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 3))
                .sample()
        );

        // when
        notificationProcessor.processStockSummary(member.getId());

        // then
        verify(notificationSender, timeout(2000)).send(
            eq(member.getId()),
            eq("재고 소진 알림 ⏰"),
            contains("우유 재고가 모두 소진되었습니다.")
        );
    }

    @Test
    void 재고_소진_알림이_꺼져있으면_알림을_발송하지_않는다() throws InterruptedException {
        // given
        notificationSettingRepository.save(notificationSetting(fixtureMonkey, member)
            .set("alertSchedule", AlertSchedule.of(LocalTime.of(9, 0), 3))
            .set("enabled", false)
            .sample());

        foodRepository.save(food(fixtureMonkey, refrigerator, member, category)
            .set("name", "음식")
            .set("expirationDate", LocalDateTime.now())
            .sample());

        // when
        notificationProcessor.processStockSummary(member.getId());

        // then
        Thread.sleep(500);
        verify(notificationSender, never()).send(anyLong(), anyString(), anyString());
    }

}