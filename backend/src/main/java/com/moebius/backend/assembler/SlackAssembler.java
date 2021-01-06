package com.moebius.backend.assembler;

import com.moebius.backend.dto.slack.SlackMessageDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.utils.OrderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackAssembler {
	private final OrderUtil orderUtil;

	public SlackMessageDto assemble(TradeSlackDto tradeSlackDto) {
		String symbol = tradeSlackDto.getSymbol();
		String unitCurrency = StringUtils.SPACE + orderUtil.getUnitCurrencyBySymbol(symbol);

		NumberFormat formatter = NumberFormat.getInstance();

		return SlackMessageDto.builder()
			.attachments(Collections.singletonList(SlackMessageDto.SlackAttachment.builder()
				.color(tradeSlackDto.getPriceChangeRate() > 0D ? "#d60000" : "#0051C7")
				.authorName(tradeSlackDto.getExchange() + "-" + symbol)
				.authorLink(tradeSlackDto.getReferenceLink())
				.text(
					"[" + symbol + "] Heavy trades (*" + (formatter.format(tradeSlackDto.getTotalValidPrice()) + unitCurrency) + "*) occurred during "
						+ tradeSlackDto.getFrom() + " ~ " + tradeSlackDto.getTo())
				.fields(buildAttachmentFields(formatter, unitCurrency, tradeSlackDto))
				.build()))
			.build();
	}

	private List<SlackMessageDto.SlackAttachment.Field> buildAttachmentFields(NumberFormat formatter, String unitCurrency,
		TradeSlackDto tradeSlackDto) {
		List<SlackMessageDto.SlackAttachment.Field> attachmentFields = new ArrayList<>();
		attachmentFields.add(SlackMessageDto.SlackAttachment.Field.builder()
			.title("Total ask price")
			.value(formatter.format((long) tradeSlackDto.getTotalAskPrice()) + unitCurrency)
			.build());
		attachmentFields.add(SlackMessageDto.SlackAttachment.Field.builder()
			.title("Total bid price")
			.value(formatter.format((long) tradeSlackDto.getTotalBidPrice()) + unitCurrency)
			.build());
		attachmentFields.add(SlackMessageDto.SlackAttachment.Field.builder()
			.title("Current price (Change rate)")
			.value(formatter.format(tradeSlackDto.getPrice()) + unitCurrency + " (" + tradeSlackDto.getPriceChangeRate() + "%)")
			.build());
		attachmentFields.add(SlackMessageDto.SlackAttachment.Field.builder()
			.title("Subscribers")
			.value(tradeSlackDto.getSubscribers())
			.build());

		return attachmentFields;
	}
}
