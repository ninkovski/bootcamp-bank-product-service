package com.nttdata.bootcamp_bank_product_service.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Response<T> {
    private String message;
    private T data;
}
