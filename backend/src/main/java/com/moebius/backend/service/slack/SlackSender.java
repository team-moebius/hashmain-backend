package com.moebius.backend.service.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@RequiredArgsConstructor
public abstract class SlackSender<T> {
	private final WebClient webClient;

	protected abstract String getWebHookUrl();

	public void sendMessage(T message) {
		log.info("[Slack] Start to send slack message [{}]", message);

		webClient.post()
			.uri(getWebHookUrl())
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(message)
			.exchange()
			.subscribe();
	}
}
