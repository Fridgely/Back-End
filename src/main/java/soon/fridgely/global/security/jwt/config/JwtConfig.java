package soon.fridgely.global.security.jwt.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import soon.fridgely.global.security.jwt.properties.JwtProperties;
import soon.fridgely.global.security.jwt.provider.JwtProvider;
import soon.fridgely.global.security.jwt.provider.TokenProvider;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public TokenProvider tokenProvider(JwtProperties jwtProperties) {
        return new JwtProvider(jwtProperties);
    }

}