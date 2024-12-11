package com.nttdata.bootcamp_bank_product_service.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductType {
    private String id; // Unique ID of the product type
    private String name; // Name of the product type
    private BigDecimal amount;
    private String type; // Type of product
    private BigDecimal commission; // Commission fee associated with this product type
    private Integer transactionCount; // Number of transactions
    private BigDecimal interest;
    private String period;
}