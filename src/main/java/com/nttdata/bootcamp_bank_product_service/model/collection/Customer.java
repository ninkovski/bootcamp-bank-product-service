package com.nttdata.bootcamp_bank_product_service.model.collection;


import com.nttdata.bootcamp_bank_product_service.model.dto.CustomerType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "customers")
public class Customer {
    @Id
    private String id;
    private String name;
    private String documentNumber;
    private String customerType;
    private String email;
    private String phoneNumber;
}