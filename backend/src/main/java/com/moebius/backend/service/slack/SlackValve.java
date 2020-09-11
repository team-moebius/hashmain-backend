package com.moebius.backend.service.slack;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SlackValve {
	private static final Map<String, LocalDateTime> LAST_SENDING_HISTORY = new ConcurrentHashMap<>();

	public void updateHistory(String key) {
		LAST_SENDING_HISTORY.put(key, LocalDateTime.now());
	}

	public boolean canSend(String key, long minuteInterval) {
		LocalDateTime sentLastAt = LAST_SENDING_HISTORY.get(key);

		return sentLastAt == null || sentLastAt.plusMinutes(minuteInterval).isBefore(LocalDateTime.now());
	}
}
