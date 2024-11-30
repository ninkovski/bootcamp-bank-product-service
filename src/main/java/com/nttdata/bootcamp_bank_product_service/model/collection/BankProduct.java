package com.nttdata.bootcamp_bank_product_service.model.collection;

import com.nttdata.bootcamp_bank_product_service.model.dto.AccountHolder;
import com.nttdata.bootcamp_bank_product_service.model.dto.Transaction;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Document(collection = "bank_products")
@Data
public class BankProduct {
    @Id
    private String id;  // Unique ID of the product
    private String productNumber;  // Unique product number
    private String typeProductId;  // Reference to the product type (ProductType)
    private boolean isActive;  // Product status (active or not)
    private BigDecimal balance;  // Product balance

    private List<AccountHolder> accountHolders;  // List of holders (titulares) for this product
    private List<Transaction> transactions;  // List of transactions (movements) for this product
}
