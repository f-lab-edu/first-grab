package com.firstgrab.domain.order.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static com.firstgrab.global.config.KafkaTopicConfig.ORDER_REQUESTS_DLT;

@Slf4j
@Component
public class OrderDltConsumer {

    private static final String GROUP_ID = "order-dlt-logger";

    @KafkaListener(topics = ORDER_REQUESTS_DLT, groupId = GROUP_ID)
    public void consume(ConsumerRecord<String, String> record) {
        log.error("Order message moved to DLT, key={}, exception={}, message={}, value={}",
                record.key(),
                headerValue(record, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN),
                headerValue(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                record.value());
    }

    private String headerValue(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
