package com.nttdata.bootcamp_bank_product_service.config;

import com.nttdata.bootcamp_bank_product_service.model.dto.CustomerType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "customer")
public class CustomerTypeConfig {
    private Map<String, CustomerType> type = new HashMap<>();
}
