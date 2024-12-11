package com.nttdata.bootcamp_bank_product_service.client;

import com.nttdata.bootcamp_bank_product_service.config.ServiceUrlsConfig;
import com.nttdata.bootcamp_bank_product_service.model.dto.ProductType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ProductTypeClient {
    private final WebClient.Builder webClientBuilder;
    private final ServiceUrlsConfig serviceUrlsConfig;

    public ProductTypeClient(WebClient.Builder webClientBuilder, ServiceUrlsConfig serviceUrlsConfig) {
        this.webClientBuilder = webClientBuilder;
        this.serviceUrlsConfig = serviceUrlsConfig;
    }

    public Mono<ProductType> findById(String id,String bearerToken) {
        return webClientBuilder.baseUrl(serviceUrlsConfig.getProductType()) // Cambia al host de tu microservicio
                .defaultHeader("Authorization", "Bearer " + bearerToken) // Añades el Bearer Token aquí
                .build()
                .get()
                .uri("/api/product-types/{id}", id)
                .retrieve()
                .bodyToMono(ProductType.class); // Devuelve un Mono de tipo ProductType
    }
}
