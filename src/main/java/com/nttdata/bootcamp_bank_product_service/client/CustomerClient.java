package com.nttdata.bootcamp_bank_product_service.client;

import com.nttdata.bootcamp_bank_product_service.config.ServiceUrlsConfig;
import com.nttdata.bootcamp_bank_product_service.model.dto.Customer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class CustomerClient {

    private final WebClient.Builder webClientBuilder;
    private final ServiceUrlsConfig serviceUrlsConfig;

    public CustomerClient(WebClient.Builder webClientBuilder, ServiceUrlsConfig serviceUrlsConfig) {
        this.webClientBuilder = webClientBuilder;
        this.serviceUrlsConfig = serviceUrlsConfig;
    }

    public Flux<Customer> findByIdIn(List<String> customerIds){
        return webClientBuilder.baseUrl(serviceUrlsConfig.getCustomer())
                .build()
                .post()
                .uri("/api/customers/in")
                .bodyValue(customerIds)
                .retrieve()
                .bodyToFlux(Customer.class);
    }
}