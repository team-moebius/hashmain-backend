package com.moebius.backend.service.slack;

import com.moebius.backend.assembler.SlackAssembler;
import com.moebius.backend.dto.slack.SlackMessageDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TradeSlackSender extends SlackSender<TradeSlackDto> {
	@Value("${slack.web-hook-url.trade}")
	private String webHookUrl;
	private final SlackAssembler slackAssembler;

	public TradeSlackSender(WebClient webClient, SlackAssembler slackAssembler) {
		super(webClient);
		this.slackAssembler = slackAssembler;
	}

	@Override
	protected SlackMessageDto getMessage(TradeSlackDto messageSource) {
		return slackAssembler.assemble(messageSource);
	}

	@Override
	protected String getWebHookUrl() {
		return webHookUrl;
	}

}
