package soon.fridgely.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import soon.fridgely.global.security.properties.JwtProperties;
import soon.fridgely.global.security.provider.JwtProvider;
import soon.fridgely.global.security.provider.TokenProvider;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public TokenProvider tokenProvider(JwtProperties jwtProperties) {
        return new JwtProvider(jwtProperties);
    }

}