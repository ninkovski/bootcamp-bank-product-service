package com.nttdata.bootcamp_bank_product_service.model.dto;

import lombok.Data;

@Data
public class ProductType {
    private String id; // Unique ID of the product type
    private String name; // Name of the product type
    private String type; // Type of product
    private double commission; // Commission fee associated with this product type
    private Integer transactionCount; // Number of transactions
}