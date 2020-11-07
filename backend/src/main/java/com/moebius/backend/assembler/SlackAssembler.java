package com.moebius.backend.assembler;

import com.moebius.backend.dto.slack.SlackMessageDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.utils.OrderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class SlackAssembler {
	private final OrderUtil orderUtil;

	public SlackMessageDto assemble(TradeSlackDto tradeSlackDto) {
		String symbol = tradeSlackDto.getSymbol();
		String unitCurrency = orderUtil.getUnitCurrencyBySymbol(symbol);
		String targetCurrency = orderUtil.getTargetCurrencyBySymbol(symbol);

		NumberFormat formatter = NumberFormat.getInstance();

		return SlackMessageDto.builder()
			.attachments(Collections.singletonList(SlackMessageDto.SlackAttachment.builder()
				.color(tradeSlackDto.getPriceChangeRate() > 0D ? "#d60000" : "#0051C7")
				.authorName(tradeSlackDto.getExchange() + "-" + symbol)
				.authorLink(tradeSlackDto.getReferenceLink())
				.text("[" + symbol + "] Heavy trades(*" + (formatter.format(tradeSlackDto.getTotalValidPrice()) + unitCurrency) + "*) occurred during "
					+ tradeSlackDto.getFrom() + " ~ " + tradeSlackDto.getTo())
				.fields(Arrays.asList(SlackMessageDto.SlackAttachment.Field.builder()
						.title("Total ask price")
						.value(formatter.format(tradeSlackDto.getTotalAskPrice()) + targetCurrency)
						.build(),
					SlackMessageDto.SlackAttachment.Field.builder()
						.title("Total bid price")
						.value(formatter.format(tradeSlackDto.getTotalBidPrice()) + targetCurrency)
						.build(),
					SlackMessageDto.SlackAttachment.Field.builder()
						.title("Current price (Change rate)")
						.value(formatter.format(tradeSlackDto.getPrice()) + unitCurrency + " (" + tradeSlackDto.getPriceChangeRate() + "%)")
						.build()))
				.build()))
			.build();
	}
}
