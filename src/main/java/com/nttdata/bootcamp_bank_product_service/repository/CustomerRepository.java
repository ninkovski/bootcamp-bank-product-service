package com.nttdata.bootcamp_bank_product_service.repository;

import com.nttdata.bootcamp_bank_product_service.model.collection.Customer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface CustomerRepository extends ReactiveMongoRepository<Customer, String> {
    Flux<Customer> findByIdIn(Collection<String> ids);
}