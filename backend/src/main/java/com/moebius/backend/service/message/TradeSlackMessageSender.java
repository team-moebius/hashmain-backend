package com.moebius.backend.service.message;

import com.moebius.backend.dto.message.MessageDedupStrategy;
import com.moebius.backend.dto.message.MessageRecipientType;
import com.moebius.backend.dto.slack.TradeSlackBodyDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.service.kafka.producer.MessageKafkaProducer;
import com.moebius.backend.utils.OrderUtil;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class TradeSlackMessageSender extends MessageSender<TradeSlackDto, TradeSlackBodyDto> {
    private static final String TITLE_FORMAT = "%s-%s-%s-%s";
    private static final String TEMPLATE_NAME = "trade_alert";
    //Just for dual write, after migration completed, will be changed to trade-alert
    private static final String RECIPIENT_SLACK_ID = "message-test";
    private static final DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderUtil orderUtil;

    public TradeSlackMessageSender(MessageKafkaProducer messageKafkaProducer, OrderUtil orderUtil) {
        super(messageKafkaProducer);
        this.orderUtil = orderUtil;
    }

    @Override
    protected long getDedupPeriod() {
        return 3;
    }

    @Override
    protected MessageDedupStrategy getDedupStrategy() {
        return MessageDedupStrategy.LEAVE_LAST_ARRIVAL;
    }

    @Override
    protected String getTitle(TradeSlackDto param) {
        return String.format(TITLE_FORMAT,
                param.getSymbol(), param.getTotalValidPrice(), param.getFrom(), param.getTo()
        );
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
                .symbol(symbol)
                .exchange(param.getExchange().name())
                .totalAskVolume(param.getTotalAskVolume())
                .totalBidVolume(param.getTotalBidVolume())
                .totalValidPrice(param.getTotalValidPrice())
                .price(param.getPrice())
                .priceChangeRate(param.getPriceChangeRate())
                .unitCurrency(unitCurrency)
                .targetCurrency(targetCurrency)
                .from(param.getFrom().format(LOCAL_TIME_FORMAT))
                .to(param.getTo().format(LOCAL_TIME_FORMAT))
                .referenceLink(param.getReferenceLink())
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
