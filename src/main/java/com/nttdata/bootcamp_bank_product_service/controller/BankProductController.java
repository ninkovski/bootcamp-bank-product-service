package com.nttdata.bootcamp_bank_product_service.controller;

import com.nttdata.bootcamp_bank_product_service.model.collection.BankProduct;
import com.nttdata.bootcamp_bank_product_service.model.dto.Transaction;
import com.nttdata.bootcamp_bank_product_service.model.response.Response;
import com.nttdata.bootcamp_bank_product_service.service.BankProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/bankproducts")
public class BankProductController {

    private final BankProductService bankProductService;

    public BankProductController(BankProductService bankProductService) {
        this.bankProductService = bankProductService;
    }

    // Crear un producto bancario
    @PostMapping
    public Mono<ResponseEntity<Response<BankProduct>>> createBankProduct(@RequestBody BankProduct bankProduct) {
        return bankProductService.createBankProduct(bankProduct);
    }

    // Consultar todos los productos bancarios
    @GetMapping
    public Flux<ResponseEntity<Response<BankProduct>>> getAllBankProducts() {
        return bankProductService.getAllBankProducts();
    }

    // Consultar un producto bancario por ID
    @GetMapping("/{productId}")
    public Mono<ResponseEntity<Response<BankProduct>>> getBankProduct(@PathVariable String productId) {
        return bankProductService.getBankProductById(productId);
    }

    // Actualizar un producto bancario
    /*@PutMapping("/{productId}")
    public Mono<ResponseEntity<Response<BankProduct>>> updateBankProduct(@PathVariable String productId, @RequestBody BankProduct bankProduct) {
        return bankProductService.updateBankProduct(productId, bankProduct);
    }*/

    // Eliminar un producto bancario
    @DeleteMapping("/{productId}")
    public Mono<ResponseEntity<Response<Object>>> deleteBankProduct(@PathVariable String productId) {
        return bankProductService.deleteBankProduct(productId);
    }

    // Realizar una transacción (depósito o retiro)
    @PostMapping("/{productId}/transaction")
    public Mono<ResponseEntity<Response<BankProduct>>> makeTransaction(@PathVariable String productId, @RequestBody Transaction transaction) {
        return bankProductService.makeTransaction(productId, transaction);
    }

    // Consultar saldo de un producto
    @GetMapping("/{productId}/balance")
    public Mono<ResponseEntity<Response<BigDecimal>>> getProductBalance(@PathVariable String productId) {
        return bankProductService.getProductBalance(productId);
    }

    // Consultar todos los movimientos de un producto bancario
    @GetMapping("/{productId}/transactions")
    public Mono<ResponseEntity<Response<List<Transaction>>>> getProductTransactions(@PathVariable String productId) {
        return bankProductService.getProductTransactions(productId);
    }
}