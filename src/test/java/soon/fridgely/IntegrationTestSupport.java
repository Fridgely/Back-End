package soon.fridgely;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@TruncateTables
@SpringBootTest
public abstract class IntegrationTestSupport {
}