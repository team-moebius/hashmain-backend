package com.moebius.backend.service.trade;

import com.moebius.backend.service.kafka.consumer.TradeKafkaConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeService implements ApplicationListener<ApplicationReadyEvent> {
	private final TradeKafkaConsumer tradeKafkaConsumer;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		tradeKafkaConsumer.consumeMessages();
	}
}
