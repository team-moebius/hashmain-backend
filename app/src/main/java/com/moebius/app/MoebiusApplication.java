package com.moebius.app;

import com.moebius.backend.domain.commons.Exchange;
import com.moebius.backend.service.kafka.consumer.UpbitKafkaConsumer;
import com.moebius.backend.service.market.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.Arrays;

@SpringBootApplication
@RequiredArgsConstructor
public class MoebiusApplication implements ApplicationListener<ApplicationReadyEvent> {
    private final MarketService marketService;
    private final UpbitKafkaConsumer upbitKafkaConsumer;

    public static void main(String[] args) {
        SpringApplication.run(MoebiusApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Arrays.stream(Exchange.values()).forEach(marketService::updateMarkets);
        upbitKafkaConsumer.consumeMessages();
    }
}
