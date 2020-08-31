package com.moebius.backend.service.slack;

import com.moebius.backend.assembler.SlackAssembler;
import com.moebius.backend.dto.TradeDto;
import com.moebius.backend.dto.slack.SlackMessageDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;

@Component
public class TradeSlackSender extends SlackSender<TradeSlackDto> {
	@Value("${slack.web-hook-url.trade}")
	private String webHookUrl;
	@Value("${exchange.")
	private String upbitBase;
	private final SlackAssembler slackAssembler;

	public TradeSlackSender(WebClient webClient, SlackAssembler slackAssembler) {
		super(webClient);
		this.slackAssembler = slackAssembler;
	}

	@Override
	protected SlackMessageDto getMessage(TradeSlackDto messageSource) {
		TradeDto tradeDto = messageSource.getTradeDto();

		return SlackMessageDto.builder()
			.attachments(Collections.singletonList(SlackMessageDto.SlackAttachment.builder()
				.color(messageSource.getUpdatedChangeRate() > 0f ? "#d60000" : "#0051C7")
				.authorName(tradeDto.getSymbol())
				.authorLink()
				.text()
				.fields()
				.build()))
			.build();
	}

	@Override
	protected String getWebHookUrl() {
		return webHookUrl;
	}

}
