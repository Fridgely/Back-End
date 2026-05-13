package soon.fridgely.global.support;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@TruncateTables
@SpringBootTest
public abstract class IntegrationTestSupport {

    // Singleton Container: JVM 내 한 번만 시작 → Spring 컨텍스트 캐싱과 충돌 없음
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    protected final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

}
