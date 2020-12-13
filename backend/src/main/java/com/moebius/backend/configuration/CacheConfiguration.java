package com.moebius.backend.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfiguration {
	@CacheEvict(allEntries = true, value = "upbitTradeMeta")
	@Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 500)
	public void evictUpbitTradeMeta() {
		log.info("[Upbit] Evict cache for upbit trade meta.");
	}

	@CacheEvict(allEntries = true, value = "apiKeyWithAssets")
	@Scheduled(fixedDelay = 60 * 1000, initialDelay = 500)
	public void evictApiKeyWithAssets() {
		log.info("[Asset] Evict cache for api key with assets.");
	}
}
