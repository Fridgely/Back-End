package soon.fridgely.global.support;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.*;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;

import java.util.Arrays;

public abstract class FixtureMonkeyFactory {

    public static FixtureMonkey get() {
        return FixtureMonkey.builder()
            .objectIntrospector(new FailoverIntrospector(
                Arrays.asList(
                    ConstructorPropertiesArbitraryIntrospector.INSTANCE, // 생성자 프로퍼티
                    BuilderArbitraryIntrospector.INSTANCE, // 빌더 패턴
                    FieldReflectionArbitraryIntrospector.INSTANCE, // 필드 리플렉션
                    BeanArbitraryIntrospector.INSTANCE // 자바빈즈 규약(getter/setter)
                ),
                false // 로깅 비활성화
            ))
            .plugin(new JakartaValidationPlugin()) // 유효성 검사 적용
            .defaultNotNull(true) // null 방지
            .build();
    }

}