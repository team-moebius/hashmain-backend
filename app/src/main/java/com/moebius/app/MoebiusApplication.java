package com.moebius.app;

import com.moebius.backend.service.kafka.consumer.UpbitKafkaConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
@RequiredArgsConstructor
public class MoebiusApplication implements ApplicationListener<ApplicationReadyEvent> {
    private final UpbitKafkaConsumer upbitKafkaConsumer;

    public static void main(String[] args) {
        SpringApplication.run(MoebiusApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        upbitKafkaConsumer.consumeMessages();
    }
}
