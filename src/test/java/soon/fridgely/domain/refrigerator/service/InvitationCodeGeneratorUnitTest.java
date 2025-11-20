package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationCodeGeneratorUnitTest {

    private final InvitationCodeGenerator generator = new InvitationCodeGenerator();

    @Test
    void 생성된_코드는_8자리이며_대문자와_숫자로_구성되어있다() {
        // given
        Pattern validPattern = Pattern.compile("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$");

        // when
        String code = generator.generate();

        // then
        assertThat(code).matches(validPattern);
    }

    @Test
    void 다수의_코드를_생성할때_중복이_발생하지_않아야한다() {
        // given
        int tryCount = 100_000;
        Set<String> codes = new HashSet<>(tryCount);

        // when
        for (int i = 0; i < tryCount; i++) {
            String code = generator.generate();
            codes.add(code);
        }

        // then
        assertThat(codes).hasSize(tryCount);
    }
}