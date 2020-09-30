package com.moebius.backend.service.slack;

import com.moebius.backend.assembler.SlackAssembler;
import com.moebius.backend.dto.slack.SlackMessageDto;
import com.moebius.backend.dto.slack.TradeSlackDto;
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
	private final SlackValve slackValve;
	private final long MINUTE_INTERVAL = 5L;

	public TradeSlackSender(WebClient webClient, SlackAssembler slackAssembler, SlackValve slackValve) {
		super(webClient);
		this.slackAssembler = slackAssembler;
		this.slackValve = slackValve;
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
		String valveKey = messageSource.getExchange() + "-" + messageSource.getSymbol();

		if (slackValve.canSend(valveKey, MINUTE_INTERVAL)) {
			return super.sendMessage(messageSource)
				.doOnSuccess(clientResponse -> slackValve.updateHistory(valveKey));
		}

		return Mono.empty();
	}
}
