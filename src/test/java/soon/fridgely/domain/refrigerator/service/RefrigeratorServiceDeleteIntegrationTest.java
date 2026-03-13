package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static soon.fridgely.global.support.fixture.CategoryFixture.category;
import static soon.fridgely.global.support.fixture.FoodFixture.food;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class RefrigeratorServiceDeleteIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RefrigeratorService refrigeratorService;

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

    private Member owner;
    private Refrigerator refrigerator;

    @BeforeEach
    void setUp() {
        this.owner = memberRepository.save(member(fixtureMonkey).sample());
        this.refrigerator = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, owner)
                .set("role", RefrigeratorRole.OWNER)
                .sample()
        );
    }

    @Test
    void OWNER가_냉장고를_삭제하면_냉장고와_연관_데이터가_모두_삭제된다() {
        // given
        Member guestMember = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, guestMember)
                .set("role", RefrigeratorRole.MEMBER)
                .sample()
        );

        var savedCategory = categoryRepository.save(category(fixtureMonkey, refrigerator, owner).sample());
        foodRepository.save(food(fixtureMonkey, refrigerator, owner, savedCategory).sample());

        var key = new MemberRefrigeratorKey(owner.getId(), refrigerator.getId());

        // when
        refrigeratorService.deleteRefrigerator(key);

        // then
        assertThat(refrigeratorRepository.findByIdAndStatus(refrigerator.getId(), EntityStatus.ACTIVE)).isEmpty();
        assertThat(foodRepository.findAllByRefrigeratorIdAndStatus(refrigerator.getId(), EntityStatus.ACTIVE)).isEmpty();
        assertThat(categoryRepository.findAllByRefrigeratorIdAndStatus(refrigerator.getId(), EntityStatus.ACTIVE)).isEmpty();
        assertThat(memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(refrigerator.getId(), owner.getId(), EntityStatus.ACTIVE)).isFalse();
        assertThat(memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(refrigerator.getId(), guestMember.getId(), EntityStatus.ACTIVE)).isFalse();
    }

    @Test
    void MEMBER가_냉장고를_삭제하려하면_예외가_발생한다() {
        // given
        Member guestMember = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, guestMember)
                .set("role", RefrigeratorRole.MEMBER)
                .sample()
        );
        var key = new MemberRefrigeratorKey(guestMember.getId(), refrigerator.getId());

        // expected
        assertThatThrownBy(() -> refrigeratorService.deleteRefrigerator(key))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ONLY_OWNER_CAN_DELETE_REFRIGERATOR);
    }

    @Test
    void 비멤버가_냉장고를_삭제하려하면_예외가_발생한다() {
        // given
        Member outsider = memberRepository.save(member(fixtureMonkey).sample());
        var key = new MemberRefrigeratorKey(outsider.getId(), refrigerator.getId());

        // expected
        assertThatThrownBy(() -> refrigeratorService.deleteRefrigerator(key))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHORIZATION_FAILED);
    }

}