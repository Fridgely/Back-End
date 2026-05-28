package soon.fridgely.domain.refrigerator.entity;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.global.support.FixtureMonkeyFactory;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberRefrigeratorUnitTest {

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    private Member member;
    private Refrigerator refrigerator;

    @BeforeEach
    void setUp() {
        member = fixtureMonkey.giveMeOne(Member.class);
        refrigerator = fixtureMonkey.giveMeOne(Refrigerator.class);
    }

    @Test
    void OWNER는_냉장고를_삭제할_수_있다() {
        // given
        MemberRefrigerator owner = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);

        // when
        ThrowingCallable action = owner::validateOwnership;

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    void OWNER가_아니면_냉장고를_삭제할_수_없다() {
        // given
        MemberRefrigerator nonOwner = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.MEMBER);

        // expected
        assertThatThrownBy(nonOwner::validateOwnership)
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ONLY_OWNER_CAN_DELETE_REFRIGERATOR);
    }

    @Test
    void MEMBER는_냉장고를_탈퇴할_수_있다() {
        // given
        MemberRefrigerator nonOwner = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.MEMBER);

        // when
        ThrowingCallable action = nonOwner::validateCanLeave;

        // then
        assertThatCode(action).doesNotThrowAnyException();
    }

    @Test
    void OWNER는_냉장고를_탈퇴할_수_없다() {
        // given
        MemberRefrigerator owner = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);

        // expected
        assertThatThrownBy(owner::validateCanLeave)
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.OWNER_CANNOT_LEAVE_REFRIGERATOR);
    }
}
