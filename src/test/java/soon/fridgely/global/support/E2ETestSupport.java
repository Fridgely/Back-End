package soon.fridgely.global.support;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import soon.fridgely.domain.auth.provider.TokenProvider;
import soon.fridgely.domain.member.entity.Member;

@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@TruncateTables
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class E2ETestSupport {

    protected final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Autowired
    protected TestRestTemplate testRestTemplate;

    @Autowired
    protected TokenProvider tokenProvider;

    /**
     * 인증 헤더가 포함된 HttpHeaders 생성
     */
    protected HttpHeaders createAuthHeaders(Member member) {
        String token = tokenProvider.generateAllToken(member.getId(), member.getRole()).accessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    /**
     * 인증 헤더가 포함된 HttpEntity 생성 (body 없음)
     */
    protected HttpEntity<Void> createAuthEntity(Member member) {
        return new HttpEntity<>(createAuthHeaders(member));
    }

    /**
     * 인증 헤더가 포함된 HttpEntity 생성 (body 포함)
     */
    protected <T> HttpEntity<T> createAuthEntity(T body, Member member) {
        return new HttpEntity<>(body, createAuthHeaders(member));
    }

}