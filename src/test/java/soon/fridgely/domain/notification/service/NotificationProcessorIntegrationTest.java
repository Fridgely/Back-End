package soon.fridgely.domain.notification.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.notification.NotificationSender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    @Test
    void 만료_예정_음식이_있으면_알림을_발송한다() {
        // given
        Member member = createMember("user1");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("과일", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        Food food = createFood(refrigerator, member, category, "사과", LocalDateTime.now().plusDays(3L));
        foodRepository.save(food);

        AlertSchedule schedule = AlertSchedule.of(LocalTime.of(9, 0), 3);
        NotificationSetting setting = createNotificationSetting(member, schedule);
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.process(setting);

        // then
        verify(notificationSender).send(eq(member.getId()), anyString(), anyString());
    }

    @Test
    void 만료_예정_음식이_없으면_알림을_발송하지_않는다() {
        // given
        Member member = createMember("user1");
        memberRepository.save(member);

        AlertSchedule schedule = AlertSchedule.of(LocalTime.of(9, 0), 3);
        NotificationSetting setting = createNotificationSetting(member, schedule);
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.process(setting);

        // then
        verify(notificationSender, never()).send(anyLong(), anyString(), anyString());
    }

    @Test
    void 알림_스케줄의_일수에_따라_정확한_날짜의_음식을_조회한다() {
        // given
        Member member = createMember("user1");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("과일", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        Food food1 = createFood(refrigerator, member, category, "사과", LocalDateTime.now().plusDays(7L));
        Food food2 = createFood(refrigerator, member, category, "바나나", LocalDateTime.now().plusDays(3L));
        foodRepository.saveAll(List.of(food1, food2));

        AlertSchedule schedule = AlertSchedule.of(LocalTime.of(9, 0), 7);
        NotificationSetting setting = createNotificationSetting(member, schedule);
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.process(setting);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationSender).send(eq(member.getId()), anyString(), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("사과");
    }

    @Test
    void 여러_음식이_만료_예정일_때_모든_음식을_포함한_알림을_발송한다() {
        // given
        Member member = createMember("user1");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("유제품", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        LocalDateTime expiryDateTime = LocalDateTime.now().plusDays(1L);
        Food food1 = createFood(refrigerator, member, category, "우유", expiryDateTime);
        Food food2 = createFood(refrigerator, member, category, "비요뜨", expiryDateTime);
        Food food3 = createFood(refrigerator, member, category, "요구르트", expiryDateTime);
        foodRepository.saveAll(List.of(food1, food2, food3));

        AlertSchedule schedule = AlertSchedule.of(LocalTime.of(9, 0), 1);
        NotificationSetting setting = createNotificationSetting(member, schedule);
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.process(setting);

        // then
        verify(notificationSender).send(eq(member.getId()), anyString(), contains("외 2개"));
    }

    @Test
    void 다른_회원의_음식은_알림에_포함되지_않는다() {
        // given
        Member member1 = createMember("user1");
        Member member2 = createMember("user2");
        memberRepository.saveAll(List.of(member1, member2));

        Refrigerator refrigerator1 = Refrigerator.register(member1.getNickname());
        Refrigerator refrigerator2 = Refrigerator.register(member2.getNickname());
        refrigeratorRepository.saveAll(List.of(refrigerator1, refrigerator2));

        MemberRefrigerator memberRefrigerator1 = MemberRefrigerator.link(member1, refrigerator1, RefrigeratorRole.OWNER);
        MemberRefrigerator memberRefrigerator2 = MemberRefrigerator.link(member2, refrigerator2, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.saveAll(List.of(memberRefrigerator1, memberRefrigerator2));

        Category category1 = Category.register("과일", refrigerator1, member1, CategoryType.DEFAULT);
        Category category2 = Category.register("과일", refrigerator2, member2, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(category1, category2));

        LocalDateTime expiryDateTime = LocalDateTime.now().plusDays(3L);
        Food food1 = createFood(refrigerator1, member1, category1, "바나나", expiryDateTime);
        Food food2 = createFood(refrigerator2, member2, category2, "사과", expiryDateTime);
        foodRepository.saveAll(List.of(food1, food2));

        AlertSchedule schedule = AlertSchedule.of(LocalTime.of(9, 0), 3);
        NotificationSetting setting = createNotificationSetting(member1, schedule);
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.process(setting);

        // then
        verify(notificationSender).send(eq(member1.getId()), anyString(), anyString());
        verify(notificationSender, never()).send(eq(member2.getId()), anyString(), anyString());
    }

    @Test
    void 정확히_N일_후_만료되는_음식만_조회한다() {
        // given
        Member member = createMember("user1");
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        MemberRefrigerator memberRefrigerator = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);
        memberRefrigeratorRepository.save(memberRefrigerator);

        Category category = Category.register("유제품", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.save(category);

        Food food1 = createFood(refrigerator, member, category, "우유", LocalDateTime.now().plusDays(3L));
        Food food2 = createFood(refrigerator, member, category, "계란", LocalDateTime.now().plusDays(2L));
        Food food3 = createFood(refrigerator, member, category, "치즈", LocalDateTime.now().plusDays(4L));
        foodRepository.saveAll(List.of(food1, food2, food3));

        AlertSchedule schedule = AlertSchedule.of(LocalTime.of(9, 0), 3);
        NotificationSetting setting = createNotificationSetting(member, schedule);
        notificationSettingRepository.save(setting);

        // when
        notificationProcessor.process(setting);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationSender).send(eq(member.getId()), anyString(), messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).contains("우유");
        assertThat(sentMessage).doesNotContain("외");
    }

    private Member createMember(String loginId) {
        return Member.builder()
            .loginId(loginId)
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

    private Food createFood(Refrigerator refrigerator, Member member, Category category, String name, LocalDateTime expirationDate) {
        return Food.register(
            refrigerator,
            member,
            name,
            category,
            new Quantity(new BigDecimal("1.0"), Unit.KG),
            expirationDate,
            StorageType.FROZEN,
            "testDescription",
            "http://example.com/image.jpg",
            LocalDate.now()
        );
    }

    private NotificationSetting createNotificationSetting(Member member, AlertSchedule schedule) {
        return NotificationSetting.builder()
            .member(member)
            .alertSchedule(schedule)
            .enabled(true)
            .build();
    }

}