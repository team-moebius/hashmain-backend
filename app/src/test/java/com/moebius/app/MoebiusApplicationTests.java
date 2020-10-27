package com.moebius.app;

import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.service.kafka.consumer.UpbitKafkaConsumer;
import com.moebius.backend.service.market.MarketService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MoebiusApplicationTests {
	@InjectMocks
	private MoebiusApplication moebiusApplication;
	@Mock
	private MarketService marketService;
	@Mock
	private UpbitKafkaConsumer upbitKafkaConsumer;
	@Mock
	private ApplicationReadyEvent applicationReadyEvent;

	@Test
	public void contextLoads() {
	}

	@Test
	public void initializeOnApplicationEvent() {
		when(marketService.updateMarkets(any(Exchange.class))).thenReturn(Mono.just(ResponseEntity.ok().build()));

		moebiusApplication.onApplicationEvent(applicationReadyEvent);

		verify(marketService, times(Exchange.values().length)).updateMarkets(any(Exchange.class));
		verify(upbitKafkaConsumer, times(1)).consumeMessages();
	}
}
