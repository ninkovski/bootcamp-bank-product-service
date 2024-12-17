package com.nttdata.bootcamp_bank_product_service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProductTypeKafkaClient {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProductTypeKafkaClient(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProductTypeId(String id) {
        kafkaTemplate.send("product-type-request", id);
        log.info(" ID enviado a Kafka: {} ", id);
    }
}
