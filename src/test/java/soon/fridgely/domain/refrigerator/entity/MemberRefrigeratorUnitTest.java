package soon.fridgely.domain.refrigerator.entity;

import com.navercorp.fixturemonkey.FixtureMonkey;
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
    void OWNER는_validateOwnership을_호출해도_예외가_발생하지_않는다() {
        // given
        MemberRefrigerator owner = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);

        // expected
        assertThatCode(owner::validateOwnership).doesNotThrowAnyException();
    }

    @Test
    void MEMBER가_validateOwnership을_호출하면_예외가_발생한다() {
        // given
        MemberRefrigerator nonOwner = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.MEMBER);

        // expected
        assertThatThrownBy(nonOwner::validateOwnership)
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ONLY_OWNER_CAN_DELETE_REFRIGERATOR);
    }

    @Test
    void MEMBER는_validateCanLeave를_호출해도_예외가_발생하지_않는다() {
        // given
        MemberRefrigerator nonOwner = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.MEMBER);

        // expected
        assertThatCode(nonOwner::validateCanLeave).doesNotThrowAnyException();
    }

    @Test
    void OWNER가_validateCanLeave를_호출하면_예외가_발생한다() {
        // given
        MemberRefrigerator owner = MemberRefrigerator.link(member, refrigerator, RefrigeratorRole.OWNER);

        // expected
        assertThatThrownBy(owner::validateCanLeave)
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.OWNER_CANNOT_LEAVE_REFRIGERATOR);
    }
}
