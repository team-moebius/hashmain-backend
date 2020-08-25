package com.moebius.backend.service.slack;

import com.moebius.backend.dto.SlackMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TradeSlackSender extends SlackSender<SlackMessageDto> {
	@Value("${slack.web-hook-url.trade}")
	private String webHookUrl;

	public TradeSlackSender(WebClient webClient) {
		super(webClient);
	}

	@Override
	protected String getWebHookUrl() {
		return webHookUrl;
	}
}
