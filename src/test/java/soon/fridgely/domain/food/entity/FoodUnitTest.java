package soon.fridgely.domain.food.entity;

import org.junit.jupiter.api.Test;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoodUnitTest {

    @Test
    void 음식을_등록하면_입력받은_기준_시간에_따라_상태가_자동으로_설정되어야_한다() {
        // given
        Member member = createMember();
        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        Category category = Category.register("채소", refrigerator, member, CategoryType.CUSTOM);
        LocalDate fixedNow = LocalDate.of(2025, 1, 1);
        LocalDateTime expirationDate = fixedNow.plusDays(5).atStartOfDay();

        // when
        Food food = Food.register(
            refrigerator,
            member,
            "시들한 당근",
            category,
            new Quantity(BigDecimal.ONE, Unit.PIECE),
            expirationDate,
            StorageType.REFRIGERATION,
            "설명",
            "http://image.url",
            fixedNow
        );

        // then
        assertThat(food.getName()).isEqualTo("시들한 당근");
        assertThat(food.getExpirationDate()).isEqualTo(expirationDate);
        assertThat(food.getFoodStatus()).isEqualTo(FoodStatus.RED);
    }

    @Test
    void 음식_등록_시_필수_값이_누락되면_예외가_발생해야_한다() {
        Member member = createMember();
        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        Category category = Category.register("채소", refrigerator, member, CategoryType.CUSTOM);
        LocalDate now = LocalDate.now();

        assertThatThrownBy(() -> Food.register(
            refrigerator,
            member,
            "이름",
            category,
            new Quantity(BigDecimal.ONE, Unit.PIECE),
            null,
            StorageType.REFRIGERATION,
            "설명",
            "url",
            now
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("expirationDate는 필수입니다.");
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

    private Food createFood(Refrigerator refrigerator, Member member, Category category, LocalDateTime expirationDate, LocalDate now) {
        return Food.register(
            refrigerator,
            member,
            "신선한 우유",
            category,
            new Quantity(new BigDecimal("1.0"), Unit.KG),
            expirationDate,
            StorageType.FROZEN,
            "testDescription",
            "http://example.com/image.jpg",
            now
        );
    }

}