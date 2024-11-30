package com.nttdata.bootcamp_bank_product_service.repository;

import com.nttdata.bootcamp_bank_product_service.model.collection.ProductType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;


public interface ProductTypeRepository extends ReactiveMongoRepository<ProductType, String> {

}
