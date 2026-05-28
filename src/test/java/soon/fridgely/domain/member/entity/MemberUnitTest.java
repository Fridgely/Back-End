package soon.fridgely.domain.member.entity;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import static org.assertj.core.api.Assertions.assertThat;

class MemberUnitTest {

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Test
    void 기존_이미지가_있고_다른_URL이면_isProfileImageChangedTo가_true를_반환한다() {
        // given
        Member member = fixtureMonkey.giveMeBuilder(Member.class)
            .set("profileImageUrl", "https://example.com/old.jpg")
            .sample();

        // when & then
        assertThat(member.isProfileImageChangedTo("https://example.com/new.jpg")).isTrue();
    }

    @Test
    void 기존_이미지와_같은_URL이면_isProfileImageChangedTo가_false를_반환한다() {
        // given
        String imageUrl = "https://example.com/same.jpg";
        Member member = fixtureMonkey.giveMeBuilder(Member.class)
            .set("profileImageUrl", imageUrl)
            .sample();

        // when & then
        assertThat(member.isProfileImageChangedTo(imageUrl)).isFalse();
    }

    @Test
    void 기존_이미지가_없으면_isProfileImageChangedTo가_false를_반환한다() {
        // given
        Member member = fixtureMonkey.giveMeBuilder(Member.class)
            .set("profileImageUrl", null)
            .sample();

        // when & then
        assertThat(member.isProfileImageChangedTo("https://example.com/new.jpg")).isFalse();
    }

    @Test
    void 기존_이미지가_빈_문자열이면_isProfileImageChangedTo가_false를_반환한다() {
        // given
        Member member = fixtureMonkey.giveMeBuilder(Member.class)
            .set("profileImageUrl", "")
            .sample();

        // when & then
        assertThat(member.isProfileImageChangedTo("https://example.com/new.jpg")).isFalse();
    }
}
