package com.moebius.backend.service.kafka.producer;

import com.moebius.backend.dto.trade.TradeDto;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.SenderResult;

import java.util.Map;

@Component
public class TradeKafkaProducer extends KafkaProducer<String, TradeDto, String> {
	private static final String TRADE_KAFKA_TOPIC = "moebius.trade.upbit";

	public TradeKafkaProducer(Map<String, String> senderDefaultProperties) {
		super(senderDefaultProperties);
	}

	@Override
	public String getTopic() {
		return TRADE_KAFKA_TOPIC;
	}

	@Override
	protected Class<?> getKeySerializerClass() {
		return StringSerializer.class;
	}

	@Override
	protected Class<?> getValueSerializerClass() {
		return JsonSerializer.class;
	}

	@Override
	protected String getKey(TradeDto message) {
		return message.getSymbol();
	}

	@Override
	protected String getCorrelationMetadata(TradeDto message) {
		return TRADE_KAFKA_TOPIC + "." + message.getSymbol();
	}

	@Override
	public Flux<SenderResult<String>> produceMessages(TradeDto message) {
		return super.produceMessages(message);
	}
}
