package com.nttdata.bootcamp_bank_product_service.service;

import static org.mockito.Mockito.*;

import com.nttdata.bootcamp_bank_product_service.client.CustomerClient;
import com.nttdata.bootcamp_bank_product_service.client.ProductTypeClient;
import com.nttdata.bootcamp_bank_product_service.config.ProductTypeConfig;
import com.nttdata.bootcamp_bank_product_service.model.collection.BankProduct;
import com.nttdata.bootcamp_bank_product_service.model.dto.AccountHolder;
import com.nttdata.bootcamp_bank_product_service.model.dto.Transaction;
import com.nttdata.bootcamp_bank_product_service.model.dto.Customer;
import com.nttdata.bootcamp_bank_product_service.repository.BankProductRepository;
import com.nttdata.bootcamp_bank_product_service.service.impl.BankProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

class BankProductServiceImplTest {

    @Mock private BankProductRepository bankProductRepository;
    @Mock private ProductTypeClient productTypeClient;
    @Mock private CustomerClient customerClient;
    @Mock private ProductTypeConfig productTypeConfig;

    private BankProductServiceImpl bankProductService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bankProductService = new BankProductServiceImpl(
                bankProductRepository,
                productTypeClient,
                customerClient,
                productTypeConfig);
    }

    @Test
    void testCreateBankProduct_MissingAccountHolders() {
        BankProduct bankProduct = new BankProduct();
        bankProduct.setTypeProductId("validTypeId");
        bankProduct.setAccountHolders(null);

        StepVerifier.create(bankProductService.createBankProduct(bankProduct, "bearerToken"))
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.BAD_REQUEST &&
                                "Bank product must have at least one account holder.".equals(Objects.requireNonNull(response.getBody()).getMessage())
                )
                .verifyComplete();
    }

}
