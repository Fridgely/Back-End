package soon.fridgely.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import soon.fridgely.global.support.properties.S3Properties;
import soon.fridgely.global.support.provider.S3Provider;
import soon.fridgely.global.support.provider.StorageProvider;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class StorageConfig {

    @Bean
    public StorageProvider storageProvider(S3Properties s3Properties) {
        return new S3Provider(s3Properties);
    }

}