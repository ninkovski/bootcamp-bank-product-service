package com.nttdata.bootcamp_bank_product_service.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Transaction {
    private String productId;  // Reference to the bank product (account)
    private Boolean substract;  // Transaction amount (positive for deposits, negative for withdrawals)
    private BigDecimal amount;  // Transaction amount (positive for deposits, negative for withdrawals)
    private String date;  // Transaction date (ISO format or String)
    private String description;
}
