package com.moebius.backend.service.kafka.consumer;

import com.moebius.backend.dto.SlackMessageDto;
import com.moebius.backend.dto.TradeDto;
import com.moebius.backend.service.market.MarketService;
import com.moebius.backend.service.order.ExchangeOrderService;
import com.moebius.backend.service.slack.TradeSlackSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Map;

@Slf4j
@Component
public class TradeKafkaConsumer extends KafkaConsumer<String, TradeDto> {
	private static final String TRADE_KAFKA_TOPIC = "moebius.trade.upbit";
	private final ExchangeOrderService exchangeOrderService;
	private final MarketService marketService;
	private final TradeSlackSender tradeSlackSender;

	public TradeKafkaConsumer(Map<String, String> receiverDefaultProperties, ExchangeOrderService exchangeOrderService, MarketService marketService,
		TradeSlackSender tradeSlackSender) {
		super(receiverDefaultProperties);
		this.exchangeOrderService = exchangeOrderService;
		this.marketService = marketService;
		this.tradeSlackSender = tradeSlackSender;
	}

	@Override
	public String getTopic() {
		return TRADE_KAFKA_TOPIC;
	}

	@Override
	public void processRecord(ReceiverRecord<String, TradeDto> record) {
		ReceiverOffset offset = record.receiverOffset();
		TradeDto tradeDto = record.value();

		exchangeOrderService.updateOrderStatus(tradeDto);
		exchangeOrderService.orderWithTradeDto(tradeDto);
		marketService.updateMarketPrice(tradeDto);
		tradeSlackSender.sendMessage(SlackMessageDto.builder().build());

		offset.acknowledge();
	}

	@Override
	protected Class<?> getKeyDeserializerClass() {
		return StringDeserializer.class;
	}

	@Override
	protected Class<?> getValueDeserializerClass() {
		return JsonDeserializer.class;
	}
}
