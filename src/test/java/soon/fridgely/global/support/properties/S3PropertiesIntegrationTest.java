package soon.fridgely.global.support.properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

class S3PropertiesIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private S3Properties s3Properties;

    @Test
    void 설정파일의_S3_프로퍼티가_정상적으로_바인딩된다() {
        // expected
        assertThat(s3Properties.credentials().accessKey()).isNotNull();
        assertThat(s3Properties.credentials().secretKey()).isNotNull();
        assertThat(s3Properties.region().staticRegion()).isNotNull();
        assertThat(s3Properties.s3().bucket()).isNotNull();
    }

}