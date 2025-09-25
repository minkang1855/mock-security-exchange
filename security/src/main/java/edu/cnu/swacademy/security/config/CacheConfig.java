package edu.cnu.swacademy.security.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 로컬 캐시 설정
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // 캐시 이름들 정의 (cashBalance와 histories 캐시)
        cacheManager.setCacheNames(Arrays.asList(
            "cashBalance"       // 현금 잔고 조회 및 입출금 내역 조회
        ));
        
        return cacheManager;
    }
}