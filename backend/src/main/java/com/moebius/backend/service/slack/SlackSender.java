package com.moebius.backend.service.slack;

import com.moebius.backend.dto.slack.SlackMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@RequiredArgsConstructor
public abstract class SlackSender<D> {
	private final WebClient webClient;

	protected abstract SlackMessageDto getMessage(D messageSource);

	protected abstract String getWebHookUrl();

	public final void sendMessage(D messageSource) {
		SlackMessageDto message = getMessage(messageSource);
		log.info("[Slack] Start to send slack message [{}]", message);

		webClient.post()
			.uri(getWebHookUrl())
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(message)
			.exchange()
			.subscribe();
	}
}
