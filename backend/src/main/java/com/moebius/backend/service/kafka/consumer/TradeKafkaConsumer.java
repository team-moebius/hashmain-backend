package com.moebius.backend.service.kafka.consumer;

import com.moebius.backend.dto.TradeDto;
import com.moebius.backend.service.market.MarketService;
import com.moebius.backend.service.order.ExchangeOrderService;
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

	public TradeKafkaConsumer(Map<String, String> receiverDefaultProperties, ExchangeOrderService exchangeOrderService, MarketService marketService) {
		super(receiverDefaultProperties);
		this.exchangeOrderService = exchangeOrderService;
		this.marketService = marketService;
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
