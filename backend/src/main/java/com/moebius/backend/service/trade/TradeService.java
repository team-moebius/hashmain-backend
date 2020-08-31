package com.moebius.backend.service.trade;

import com.moebius.backend.dto.OrderDto;
import com.moebius.backend.dto.TradeDto;
import com.moebius.backend.service.kafka.consumer.TradeKafkaConsumer;
import com.moebius.backend.service.slack.TradeSlackSender;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.util.Precision;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TradeService implements ApplicationListener<ApplicationReadyEvent> {
	private final TradeKafkaConsumer tradeKafkaConsumer;
	private final TradeSlackSender tradeSlackSender;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		tradeKafkaConsumer.consumeMessages();
	}

	public void identifyValidTrade(TradeDto tradeDto) {

	}

	private Mono<OrderDto> getOrderWhenIdentifyValidTrade(TradeDto tradeDto) {
		double changeRate = Precision.round(tradeDto.getPrice() / tradeDto.getPrevClosingPrice() - 1, 4) * 100;

		if (changeRate >= 2.0f) {

		} else if (changeRate <= -2.0f) {

		}

		return Mono.empty();
	}
}
