package com.nttdata.bootcamp_bank_product_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Customer {
    private String id;
    private String name;
    private String documentNumber;
    private String customerType;
    private String email;
    private String phoneNumber;
}