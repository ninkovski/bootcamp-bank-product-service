package com.nttdata.bootcamp_bank_product_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "url.service")
@Data
public class ServiceUrlsConfig {

    private String customer;
    private String bankProduct;
    private String productType;
}