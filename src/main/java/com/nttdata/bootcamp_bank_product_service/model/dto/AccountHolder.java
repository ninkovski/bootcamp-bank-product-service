package com.nttdata.bootcamp_bank_product_service.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;


@Data
@AllArgsConstructor
public class AccountHolder {
    private String customerId;
    private Boolean isHolder;
}
