package com.logistics.dashboard.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "overview",
                "inventory-alerts",
                "shipment-status",
                "kpi-trend"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(32)
                .maximumSize(500)
                .expireAfterWrite(Duration.ofSeconds(15))
                .recordStats());
        return cacheManager;
    }
}
