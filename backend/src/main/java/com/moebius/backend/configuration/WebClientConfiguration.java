package com.moebius.backend.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
public class WebClientConfiguration {
	@Bean
	public WebClient webClient() {
		ClientHttpConnector connector = new ReactorClientHttpConnector();

		return WebClient.builder().clientConnector(connector).build();
	}

	@Bean
	public WebSocketClient webSocketClient() {
		return new ReactorNettyWebSocketClient();
	}

	@Bean
	public ConcurrentMap<String, WebSocketSession> openedSessions() {
		return new ConcurrentHashMap<>();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
