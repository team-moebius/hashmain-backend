package com.moebius.backend.service.slack;

import com.moebius.backend.assembler.SlackAssembler;
import com.moebius.backend.dto.slack.SlackMessageDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
import com.moebius.backend.service.message.TradeSlackMessageSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class TradeSlackSender extends SlackSender<TradeSlackDto> {
	@Value("${slack.web-hook-url.trade}")
	private String webHookUrl;
	private final SlackAssembler slackAssembler;
	private final TradeSlackMessageSender tradeSlackMessageSender;

	public TradeSlackSender(WebClient webClient, SlackAssembler slackAssembler, TradeSlackMessageSender tradeSlackMessageSender) {
		super(webClient);
		this.slackAssembler = slackAssembler;
		this.tradeSlackMessageSender = tradeSlackMessageSender;
	}

	@Override
	protected SlackMessageDto getMessage(TradeSlackDto messageSource) {
		return slackAssembler.assemble(messageSource);
	}

	@Override
	protected String getWebHookUrl() {
		return webHookUrl;
	}

	@Override
	public Mono<ClientResponse> sendMessage(TradeSlackDto messageSource) {
		return super.sendMessage(messageSource)
				.flatMap(clientResponse -> tradeSlackMessageSender
						.sendMessage(messageSource)
						.map(result->clientResponse)
				);
	}
}
