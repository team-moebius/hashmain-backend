package com.moebius.backend.service.message;

import com.moebius.backend.dto.message.MessageDedupStrategy;
import com.moebius.backend.dto.message.MessageRecipientType;
import com.moebius.backend.dto.slack.TradeSlackBodyDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.service.kafka.producer.MessageKafkaProducer;
import com.moebius.backend.utils.OrderUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

@Component
public class TradeSlackMessageSender extends MessageSender<TradeSlackDto, TradeSlackBodyDto> {
    private static final long TREMENDOUS_TRADE_THRESHOLD = 100000000L;
    private static final String BLANK = " ";
    private static final String TITLE_FORMAT = "%s-%s-%s";
    private static final String TEMPLATE_NAME = "trade_alert";
    //Just for dual write, after migration completed, will be changed to trade-alert
    private static final String RECIPIENT_SLACK_ID = "message-test";
    private static final DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderUtil orderUtil;
    private final NumberFormat formatter;
    private final String[] subscribers;

    public TradeSlackMessageSender(MessageKafkaProducer messageKafkaProducer,
                                   OrderUtil orderUtil,
                                   @Value("${slack.subscribers}") String[] subscribers) {
        super(messageKafkaProducer);
        this.orderUtil = orderUtil;
        this.formatter = NumberFormat.getInstance();
        this.subscribers = subscribers;
    }

    @Override
    protected long getDedupPeriod() {
        return 1;
    }

    @Override
    protected MessageDedupStrategy getDedupStrategy() {
        return MessageDedupStrategy.LEAVE_LAST_ARRIVAL;
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

        TradeSlackBodyDto.TradeSlackBodyDtoBuilder slackMessageBodyBuilder = TradeSlackBodyDto.builder();

        slackMessageBodyBuilder.color(param.getPriceChangeRate() > 0D ? "#d60000" : "#0051C7")
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
                .referenceLink(param.getReferenceLink());

        if (Math.abs(param.getTotalValidPrice()) >= TREMENDOUS_TRADE_THRESHOLD) {
            slackMessageBodyBuilder.subscribers(String.join(BLANK, subscribers));
        }

        return slackMessageBodyBuilder.build();
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
