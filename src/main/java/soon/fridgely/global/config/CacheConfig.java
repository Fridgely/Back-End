package soon.fridgely.global.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@EnableCaching
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine")
public class CacheConfig {

    private static final int CATEGORIES_TTL_HOURS = 24;
    private static final int CATEGORIES_MAX_SIZE = 100;

    private static final int REFRIGERATORS_TTL_HOURS = 1;
    private static final int REFRIGERATORS_MAX_SIZE = 1000;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("categories", buildCache(CATEGORIES_TTL_HOURS, CATEGORIES_MAX_SIZE));
        cacheManager.registerCustomCache("myRefrigerators", buildCache(REFRIGERATORS_TTL_HOURS, REFRIGERATORS_MAX_SIZE));

        log.info("[CacheConfig] Caffeine 캐시 설정 완료 (categories: {}h, myRefrigerators: {}h)", CATEGORIES_TTL_HOURS, REFRIGERATORS_TTL_HOURS);

        return cacheManager;
    }

    private Cache<Object, Object> buildCache(int ttlHours, int maxSize) {
        return Caffeine.newBuilder()
            .expireAfterWrite(ttlHours, TimeUnit.HOURS)
            .maximumSize(maxSize)
            .recordStats() // 캐시 통계 기록 활성화
            .build();
    }

}