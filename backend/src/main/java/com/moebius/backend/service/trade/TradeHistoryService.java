package com.moebius.backend.service.trade;

import com.moebius.backend.assembler.TradeHistoryAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class TradeHistoryService {
	@Value("${moebius.data.host}")
	private String dataApiHost;
	@Value("${moebius.data.port}")
	private int dataApiPort;

	private final WebClient webClient;
	private final TradeHistoryAssembler tradeHistoryAssembler;
}
