package com.firstgrab.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_REQUESTS_TOPIC = "order-requests";
    private static final int ORDER_REQUESTS_PARTITIONS = 3;
    private static final int ORDER_REQUESTS_REPLICAS = 1;

    @Bean
    public NewTopic orderRequestsTopic() {
        return TopicBuilder.name(ORDER_REQUESTS_TOPIC)
                .partitions(ORDER_REQUESTS_PARTITIONS)
                .replicas(ORDER_REQUESTS_REPLICAS)
                .build();
    }
}
