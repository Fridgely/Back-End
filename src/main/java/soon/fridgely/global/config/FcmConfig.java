package soon.fridgely.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import soon.fridgely.global.infra.properties.FcmProperties;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
@Profile("!test")
@EnableConfigurationProperties(FcmProperties.class)
@Configuration
public class FcmConfig {

    private final FcmProperties properties;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            byte[] decodedKey = Base64.getDecoder()
                .decode(properties.base64EncodingKey());

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(decodedKey)))
                .build();
            return FirebaseApp.initializeApp(options);
        } catch (IOException | IllegalArgumentException e) {
            log.error("[FcmConfig] Firebase 초기화 실패.", e);
            throw new CoreException(ErrorType.FIREBASE_INITIALIZATION_FAILED);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

}