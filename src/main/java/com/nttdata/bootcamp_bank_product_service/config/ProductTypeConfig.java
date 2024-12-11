package com.nttdata.bootcamp_bank_product_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "product.types")
@Data
public class ProductTypeConfig {
    private String saving;
    private String current;
    private String fixed;
    private String credit;
    private String creditCard;
    private String savingVip;
    private String currentMype;
}

