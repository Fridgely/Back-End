package soon.fridgely.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import soon.fridgely.global.infra.properties.S3Properties;
import soon.fridgely.global.infra.provider.S3Provider;
import soon.fridgely.global.infra.provider.StorageProvider;

/**
 * S3 설정
 * - S3Properties: 모든 프로파일에서 활성화 (프로퍼티 검증용)
 * - S3 빈(S3Client, S3Presigner, StorageProvider): live 프로파일에서만 생성
 */
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Profile("live")
    @Bean(destroyMethod = "close")
    public S3Client s3Client(S3Properties s3Properties) {
        return S3Client.builder()
            .region(Region.of(s3Properties.region().staticRegion()))
            .credentialsProvider(buildCredentialsProvider(s3Properties))
            .build();
    }

    @Profile("live")
    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(S3Properties s3Properties) {
        return S3Presigner.builder()
            .region(Region.of(s3Properties.region().staticRegion()))
            .credentialsProvider(buildCredentialsProvider(s3Properties))
            .build();
    }

    @Profile("live")
    @Bean
    public StorageProvider storageProvider(
        S3Client s3Client,
        S3Presigner s3Presigner,
        S3Properties s3Properties
    ) {
        return new S3Provider(s3Client, s3Presigner, s3Properties);
    }

    private StaticCredentialsProvider buildCredentialsProvider(S3Properties s3Properties) {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.credentials().accessKey(), s3Properties.credentials().secretKey())
        );
    }

}