package com.moebius.backend.service.slack;

import com.moebius.backend.dto.slack.SlackMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public abstract class SlackSender<D> {
	private final WebClient webClient;

	protected abstract SlackMessageDto getMessage(D messageSource);

	protected abstract String getWebHookUrl();

	public final Mono<ClientResponse> sendMessage(D messageSource) {
		SlackMessageDto message = getMessage(messageSource);

		return webClient.post()
			.uri(getWebHookUrl())
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(message)
			.exchange();
	}
}
