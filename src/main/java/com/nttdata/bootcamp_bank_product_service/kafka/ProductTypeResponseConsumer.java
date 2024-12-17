package com.nttdata.bootcamp_bank_product_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nttdata.bootcamp_bank_product_service.model.dto.ProductType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ProductTypeResponseConsumer {

    // Cambio del tipo de Mono<ProductType> a MonoSink<ProductType>
    private final ConcurrentHashMap<String, MonoSink<ProductType>> pendingResponses = new ConcurrentHashMap<>();

    @KafkaListener(topics = "product-type-response", groupId = "product-group")
    public void listenForProductTypeResponse(String message) {
       log.info("Respuesta recibida de Kafka: {}" , message);

        ProductType productType = parseProductTypeFromMessage(message);
        if (productType != null) {
            String id = productType.getId();
            MonoSink<ProductType> sink = pendingResponses.get(id);
            if (sink != null) {
                sink.success(productType); // Completa el Mono
                pendingResponses.remove(id); // Elimina la entrada del mapa
            } else {
                log.warn("No se encontró una respuesta pendiente para el ID: " + id);
            }
        }
    }

    private ProductType parseProductTypeFromMessage(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        ProductType productType = null;
        try {
            productType = objectMapper.readValue(message, ProductType.class);
        } catch (Exception e) {
            log.error("Error al parsear el mensaje de Kafka: " + e.getMessage());
        }
        return productType;
    }

    public Mono<ProductType> waitForResponse(String id) {
        return Mono.create(sink -> pendingResponses.put(id, sink));
    }
}