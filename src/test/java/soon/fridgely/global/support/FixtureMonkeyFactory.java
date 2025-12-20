package soon.fridgely.global.support;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import net.jqwik.api.Arbitraries;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    private static final FixtureMonkey FIXTURE_MONKEY = createFixtureMonkey();

    public static FixtureMonkey get() {
        return FIXTURE_MONKEY;
    }

    private static FixtureMonkey createFixtureMonkey() {
        return FixtureMonkey.builder()
            .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE) // 기본 필드 리플렉션 인트로스펙터 설정
            .pushAssignableTypeArbitraryIntrospector( // 레코드 타입은 생성자 기반 인트로스펙터 사용
                Record.class,
                ConstructorPropertiesArbitraryIntrospector.INSTANCE
            )
            .plugin(new JakartaValidationPlugin())
            .defaultNotNull(true)
            .register(BigDecimal.class, bigDecimalFixture())
            .register(AlertSchedule.class, alertScheduleFixture())
            .register(InvitationCode.class, invitationCodeFixture())
            .build();
    }

    private static Function<FixtureMonkey, ArbitraryBuilder<BigDecimal>> bigDecimalFixture() {
        return fixture -> fixture.giveMeBuilder(BigDecimal.class)
            .set(Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, BigDecimal.valueOf(9999))
                .ofScale(2)
            );
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