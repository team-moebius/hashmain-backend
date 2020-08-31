package com.moebius.backend.assembler;

import com.moebius.backend.dto.TradeDto;
import com.moebius.backend.dto.slack.SlackMessageDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.utils.OrderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class SlackAssembler {
	@Value("${exchange.upbit.exchange.base}")
	private String upbitBase;
	private final OrderUtil orderUtil;

	public SlackMessageDto assemble(TradeSlackDto tradeSlackDto) {
		TradeDto tradeDto = tradeSlackDto.getTradeDto();
		String symbol = tradeDto.getSymbol();

		String unitCurrency = orderUtil.getUnitCurrencyBySymbol(symbol);
		String targetCurrency = orderUtil.getTargetCurrencyBySymbol(symbol);

		return SlackMessageDto.builder()
			.attachments(Collections.singletonList(SlackMessageDto.SlackAttachment.builder()
				.color(tradeSlackDto.getUpdatedChangeRate() > 0f ? "#d60000" : "#0051C7")
				.authorName(symbol)
				.authorLink(upbitBase + symbol)
				.text("Heavy trades occurred at" + tradeDto.getCreatedAt())
				.fields(Arrays.asList(SlackMessageDto.SlackAttachment.Field.builder()
						.title("Total ask volume")
						.value(tradeSlackDto.getTotalAskVolume() + targetCurrency)
						.build(),
					SlackMessageDto.SlackAttachment.Field.builder()
						.title("Total bid volume")
						.value(tradeSlackDto.getTotalBidVolume() + targetCurrency)
						.build(),
					SlackMessageDto.SlackAttachment.Field.builder()
						.title("Current price")
						.value(tradeDto.getPrice() + unitCurrency)
						.build(),
					SlackMessageDto.SlackAttachment.Field.builder()
						.title("Price change")
						.value((tradeDto.getPrice() - tradeDto.getPrevClosingPrice()) + unitCurrency + " (" + tradeSlackDto.getUpdatedChangeRate() + "%)")
						.build()))
				.build()))
			.build();
	}
}
