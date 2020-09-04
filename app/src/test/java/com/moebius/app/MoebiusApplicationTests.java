package com.moebius.app;

import com.moebius.backend.service.kafka.consumer.TradeKafkaConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MoebiusApplicationTests {
	@InjectMocks
	private MoebiusApplication moebiusApplication;
	@Mock
	private TradeKafkaConsumer tradeKafkaConsumer;
	@Mock
	private ApplicationReadyEvent applicationReadyEvent;

	@Test
	public void contextLoads() {
	}

	@Test
	public void consumeKafkaOnApplicationEvent() {
		moebiusApplication.onApplicationEvent(applicationReadyEvent);

		verify(tradeKafkaConsumer, times(1)).consumeMessages();
	}
}
