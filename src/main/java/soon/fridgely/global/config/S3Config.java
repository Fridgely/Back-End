package soon.fridgely.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import soon.fridgely.global.infra.properties.S3Properties;
import soon.fridgely.global.infra.provider.S3Provider;
import soon.fridgely.global.infra.provider.StorageProvider;

@Profile("live")
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(S3Properties s3Properties) {
        Region region = Region.of(s3Properties.region().staticRegion());
        return S3Client.builder()
            .region(region)
            .build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(S3Properties s3Properties) {
        Region region = Region.of(s3Properties.region().staticRegion());
        return S3Presigner.builder()
            .region(region)
            .build();
    }

    @Bean
    public StorageProvider storageProvider(
        S3Client s3Client,
        S3Presigner s3Presigner,
        S3Properties s3Properties
    ) {
        return new S3Provider(s3Client, s3Presigner, s3Properties);
    }

}