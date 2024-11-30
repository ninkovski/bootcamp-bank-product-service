package com.nttdata.bootcamp_bank_product_service.service;

import com.nttdata.bootcamp_bank_product_service.model.collection.BankProduct;
import com.nttdata.bootcamp_bank_product_service.model.dto.Transaction;
import com.nttdata.bootcamp_bank_product_service.model.response.Response;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

public interface BankProductService {
    Mono<ResponseEntity<Response<BankProduct>>> createBankProduct(BankProduct bankProduct);

    Flux<ResponseEntity<Response<BankProduct>>> getAllBankProducts();

    Mono<ResponseEntity<Response<BankProduct>>> getBankProductById(String productId);

    //Mono<ResponseEntity<Response<BankProduct>>> updateBankProduct(String productId, BankProduct bankProduct);

    Mono<ResponseEntity<Response<Object>>> deleteBankProduct(String productId);

    Mono<ResponseEntity<Response<BigDecimal>>> getProductBalance(String productId);

    Mono<ResponseEntity<Response<List<Transaction>>>> getProductTransactions(String productId);

    Mono<ResponseEntity<Response<BankProduct>>> makeTransaction(String productId, Transaction transaction);
}
