package soon.fridgely.global.support;

import com.navercorp.fixturemonkey.FixtureMonkey;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

@ActiveProfiles("test")
@Import({TestSecurityConfig.class, IntegrationTestSupport.NoOpShedLockConfig.class})
@TruncateTables
@SpringBootTest
public abstract class IntegrationTestSupport {

    protected final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @TestConfiguration
    static class NoOpShedLockConfig {
        @Bean
        @Primary
        public LockProvider lockProvider() {
            return lockConfiguration -> Optional.of(() -> {});
        }
    }

}
