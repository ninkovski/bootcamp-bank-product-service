package com.nttdata.bootcamp_bank_product_service.client;

import com.nttdata.bootcamp_bank_product_service.kafka.ProductTypeKafkaClient;
import com.nttdata.bootcamp_bank_product_service.kafka.ProductTypeResponseConsumer;
import com.nttdata.bootcamp_bank_product_service.model.dto.ProductType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ProductTypeClient {
    private final ProductTypeKafkaClient kafkaClient;
    private final ProductTypeResponseConsumer responseConsumer;

    public ProductTypeClient(ProductTypeKafkaClient kafkaClient, ProductTypeResponseConsumer responseConsumer) {
        this.kafkaClient = kafkaClient;
        this.responseConsumer = responseConsumer;
    }

    public Mono<ProductType> findById(String id) {
        // 1. Enviar el ID al productor Kafka
        kafkaClient.sendProductTypeId(id);
        // 2. Esperar la respuesta del consumidor
        return responseConsumer.waitForResponse(id)
                .doOnNext(productType -> log.info(" Producto recibido: " + productType));
    }
}
