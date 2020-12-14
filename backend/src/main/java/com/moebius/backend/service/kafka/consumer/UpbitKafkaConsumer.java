package com.moebius.backend.service.kafka.consumer;

import com.moebius.backend.dto.trade.TradeDto;
import com.moebius.backend.service.asset.AssetService;
import com.moebius.backend.service.market.MarketService;
import com.moebius.backend.service.order.ExchangeOrderService;
import com.moebius.backend.service.order.InternalOrderService;
import com.moebius.backend.service.trade.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Map;

@Slf4j
@Component
public class UpbitKafkaConsumer extends KafkaConsumer<String, TradeDto> {
	private static final String TRADE_KAFKA_TOPIC = "moebius.trade.upbit";
	private final ExchangeOrderService exchangeOrderService;
	private final InternalOrderService internalOrderService;
	private final MarketService marketService;
	private final TradeService tradeService;
	private final AssetService assetService;

	public UpbitKafkaConsumer(Map<String, String> receiverDefaultProperties, ExchangeOrderService exchangeOrderService,
		InternalOrderService internalOrderService, MarketService marketService,
		TradeService tradeService, AssetService assetService) {
		super(receiverDefaultProperties);
		this.exchangeOrderService = exchangeOrderService;
		this.internalOrderService = internalOrderService;
		this.marketService = marketService;
		this.tradeService = tradeService;
		this.assetService = assetService;
	}

	@Override
	public String getTopic() {
		return TRADE_KAFKA_TOPIC;
	}

	@Override
	public void processRecord(ReceiverRecord<String, TradeDto> record) {
		ReceiverOffset offset = record.receiverOffset();
		TradeDto tradeDto = record.value();

//		assetService.getApiKeyWithAssets(tradeDto).subscribe();
		internalOrderService.updateOrderStatusByTrade(tradeDto);
		exchangeOrderService.orderByTrade(tradeDto);
		marketService.updateMarketPrice(tradeDto);
		tradeService.notifyIfValidTrade(tradeDto);

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
