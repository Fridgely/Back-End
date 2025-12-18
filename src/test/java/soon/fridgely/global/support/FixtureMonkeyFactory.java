package soon.fridgely.global.support;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.*;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import net.jqwik.api.Arbitraries;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * FixtureMonkey 인스턴스를 싱글톤으로 제공하는 팩토리 클래스
 * defaultNotNull(true):
 * - 테스트에서 null 케이스는 명시적으로 setNull()로만 생성
 */
public final class FixtureMonkeyFactory {

    private FixtureMonkeyFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    // 우선순위: 필드 리플렉션 -> 생성자 프로퍼티 -> 빌더 -> 자바 빈
    private static final List<ArbitraryIntrospector> INTROSPECTORS = Arrays.asList(
        FieldReflectionArbitraryIntrospector.INSTANCE,
        ConstructorPropertiesArbitraryIntrospector.INSTANCE,
        BuilderArbitraryIntrospector.INSTANCE,
        BeanArbitraryIntrospector.INSTANCE
    );

    private static final FixtureMonkey FIXTURE_MONKEY = createFixtureMonkey();

    public static FixtureMonkey get() {
        return FIXTURE_MONKEY;
    }

    private static FixtureMonkey createFixtureMonkey() {
        return FixtureMonkey.builder()
            .objectIntrospector(new FailoverIntrospector(INTROSPECTORS, false)) // 로깅 비활성화
            .plugin(new JakartaValidationPlugin())
            .defaultNotNull(true)
            .register(Quantity.class, quantityFixture())
            .register(AlertSchedule.class, alertScheduleFixture())
            .register(InvitationCode.class, invitationCodeFixture())
            .build();
    }

    private static Function<FixtureMonkey, ArbitraryBuilder<Quantity>> quantityFixture() {
        return fixture -> fixture.giveMeBuilder(Quantity.class)
            .set("amount", Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, BigDecimal.valueOf(10000)));
    }

    private static Function<FixtureMonkey, ArbitraryBuilder<AlertSchedule>> alertScheduleFixture() {
        return fixture -> fixture.giveMeBuilder(AlertSchedule.class)
            .set("daysBeforeExpiration", Arbitraries.integers().between(1, 30))
            .set("notificationTime", Arbitraries.integers().between(0, 23)
                .map(hour -> LocalTime.of(hour, 0)));
    }

    private static Function<FixtureMonkey, ArbitraryBuilder<InvitationCode>> invitationCodeFixture() {
        return fixture -> fixture.giveMeBuilder(InvitationCode.class)
            .set("code", Arbitraries.strings()
                .withCharRange('0', '9')
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .ofLength(8))
            .set("expirationAt", Arbitraries.integers().between(1, 365)
                .map(days -> LocalDateTime.now().plusDays(days)));
    }

}