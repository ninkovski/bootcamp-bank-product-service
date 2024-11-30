package com.nttdata.bootcamp_bank_product_service.repository;

import com.nttdata.bootcamp_bank_product_service.model.collection.BankProduct;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface BankProductRepository extends ReactiveMongoRepository<BankProduct, String> {
    @Query("{ 'accountHolders.customerId': ?0 }")
    Flux<BankProduct> findAllByCustomerId(String customerId);
}