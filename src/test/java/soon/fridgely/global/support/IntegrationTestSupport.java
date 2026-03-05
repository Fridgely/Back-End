package soon.fridgely.global.support;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@TruncateTables
@SpringBootTest
public abstract class IntegrationTestSupport {

    protected final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

}