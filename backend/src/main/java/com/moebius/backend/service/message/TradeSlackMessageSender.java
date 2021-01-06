package com.moebius.backend.service.message;

import com.moebius.backend.dto.message.MessageDedupStrategy;
import com.moebius.backend.dto.message.MessageRecipientType;
import com.moebius.backend.dto.slack.TradeSlackBodyDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.service.kafka.producer.MessageKafkaProducer;
import com.moebius.backend.utils.OrderUtil;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

@Component
public class TradeSlackMessageSender extends MessageSender<TradeSlackDto, TradeSlackBodyDto> {
	private static final String TITLE_FORMAT = "%s-%s-%s";
	private static final String TEMPLATE_NAME = "trade_alert_message";
	private static final String RECIPIENT_SLACK_ID = "trade-alert";
	private static final DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	private final OrderUtil orderUtil;
	private final NumberFormat formatter;

	public TradeSlackMessageSender(MessageKafkaProducer messageKafkaProducer, OrderUtil orderUtil) {
		super(messageKafkaProducer);
		this.orderUtil = orderUtil;
		this.formatter = NumberFormat.getInstance();
	}

	@Override
	protected long getDedupPeriod() {
		return 1;
	}

	@Override
	protected MessageDedupStrategy getDedupStrategy() {
		return MessageDedupStrategy.LEAVE_FIRST_ARRIVAL;
	}

	@Override
	protected String getTitle(TradeSlackDto param) {
		return String.format(TITLE_FORMAT, param.getExchange(), param.getSymbol(), param.getPriceChangeRate() > 0D);
	}

	@Override
	protected String getTemplateName(TradeSlackDto param) {
		return TEMPLATE_NAME;
	}

	@Override
	protected TradeSlackBodyDto getBody(TradeSlackDto param) {
		String symbol = param.getSymbol();
		String unitCurrency = orderUtil.getUnitCurrencyBySymbol(symbol);
		String targetCurrency = orderUtil.getTargetCurrencyBySymbol(symbol);

		return TradeSlackBodyDto.builder()
			.color(param.getPriceChangeRate() > 0D ? "#d60000" : "#0051C7")
			.symbol(symbol)
			.exchange(param.getExchange().name())
			.totalAskPrice(formatter.format(param.getTotalAskPrice()))
			.totalBidPrice(formatter.format(param.getTotalBidPrice()))
			.totalValidPrice(formatter.format(param.getTotalValidPrice()))
			.price(formatter.format(param.getPrice()))
			.priceChangeRate(formatter.format(param.getPriceChangeRate()))
			.unitCurrency(unitCurrency)
			.targetCurrency(targetCurrency)
			.from(param.getFrom().format(LOCAL_TIME_FORMAT))
			.to(param.getTo().format(LOCAL_TIME_FORMAT))
			.referenceLink(param.getReferenceLink())
			.subscribers(param.getSubscribers())
			.build();
	}

	@Override
	protected MessageRecipientType getRecipientType() {
		return MessageRecipientType.SLACK;
	}

	@Override
	protected String getRecipientId(TradeSlackDto param) {
		return RECIPIENT_SLACK_ID;
	}
}
