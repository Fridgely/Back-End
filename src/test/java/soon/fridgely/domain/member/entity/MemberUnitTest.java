package soon.fridgely.domain.member.entity;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import static org.assertj.core.api.Assertions.assertThat;

class MemberUnitTest {

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Test
    void 기존_이미지와_다른_URL로_변경하면_변경됨으로_판단한다() {
        // given
        Member member = fixtureMonkey.giveMeBuilder(Member.class)
            .set("profileImageUrl", "https://example.com/old.jpg")
            .sample();

        // when
        boolean result = member.isProfileImageChangedTo("https://example.com/new.jpg");

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 기존_이미지와_같은_URL이면_변경되지_않음으로_판단한다() {
        // given
        String imageUrl = "https://example.com/same.jpg";
        Member member = fixtureMonkey.giveMeBuilder(Member.class)
            .set("profileImageUrl", imageUrl)
            .sample();

        // when
        boolean result = member.isProfileImageChangedTo(imageUrl);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 기존_이미지가_없으면_변경되지_않음으로_판단한다() {
        // given
        Member member = fixtureMonkey.giveMeBuilder(Member.class)
            .set("profileImageUrl", null)
            .sample();

        // when
        boolean result = member.isProfileImageChangedTo("https://example.com/new.jpg");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 기존_이미지가_빈_문자열이면_변경되지_않음으로_판단한다() {
        // given
        Member member = fixtureMonkey.giveMeBuilder(Member.class)
            .set("profileImageUrl", "")
            .sample();

        // when
        boolean result = member.isProfileImageChangedTo("https://example.com/new.jpg");

        // then
        assertThat(result).isFalse();
    }
}
