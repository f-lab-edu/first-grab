package com.firstgrab.global.config;

import com.firstgrab.global.exception.InvalidOrderMessageException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;

@Configuration
public class KafkaConsumerConfig {

    private static final double MULTIPLIER = 2.0;

    @Bean
    public RecordMessageConverter kafkaMessageConverter() {
        return new StringJacksonJsonMessageConverter();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            @Qualifier("dltKafkaTemplate") KafkaTemplate<Object, Object> dltKafkaTemplate,
            @Value("${kafka-retry.initial-interval-ms}") long initialIntervalMs,
            @Value("${kafka-retry.max-interval-ms}") long maxIntervalMs,
            @Value("${kafka-retry.max-retries}") int maxRetries) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate);

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxRetries);
        backOff.setInitialInterval(initialIntervalMs);
        backOff.setMultiplier(MULTIPLIER);
        backOff.setMaxInterval(maxIntervalMs);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(InvalidOrderMessageException.class);
        return errorHandler;
    }
}
