package com.nttdata.bootcamp_bank_product_service.service.impl;

import com.nttdata.bootcamp_bank_product_service.config.ProductTypeConfig;
import com.nttdata.bootcamp_bank_product_service.model.collection.BankProduct;
import com.nttdata.bootcamp_bank_product_service.model.collection.Customer;
import com.nttdata.bootcamp_bank_product_service.model.dto.Transaction;
import com.nttdata.bootcamp_bank_product_service.model.dto.AccountHolder;
import com.nttdata.bootcamp_bank_product_service.model.response.Response;
import com.nttdata.bootcamp_bank_product_service.repository.BankProductRepository;
import com.nttdata.bootcamp_bank_product_service.repository.CustomerRepository;
import com.nttdata.bootcamp_bank_product_service.repository.ProductTypeRepository;
import com.nttdata.bootcamp_bank_product_service.service.BankProductService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
@Service
public class BankProductServiceImpl implements BankProductService {

    private final BankProductRepository bankProductRepository;
    private final ProductTypeRepository productTypeRepository;
    private final CustomerRepository customerRepository;
    private final ProductTypeConfig productTypeConfig;

    public BankProductServiceImpl(BankProductRepository bankProductRepository, ProductTypeRepository productTypeRepository, CustomerRepository customerRepository, ProductTypeConfig productTypeConfig) {
        this.bankProductRepository = bankProductRepository;
        this.productTypeRepository = productTypeRepository;
        this.customerRepository = customerRepository;
        this.productTypeConfig = productTypeConfig;
    }

    @Override
    public Mono<ResponseEntity<Response<BankProduct>>> createBankProduct(BankProduct bankProduct) {
        List<AccountHolder> accountHolders = bankProduct.getAccountHolders();

        // Verificar que el producto bancario tenga un tipo válido
        if (bankProduct.getTypeProductId() == null) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(new Response<>("El producto bancario debe tener un tipo válido.", null)));
        }

        // Inicializar la lista de transacciones si está vacía
        if (bankProduct.getTransactions() == null) {
            bankProduct.setTransactions(new ArrayList<>());
        }

        // Establecer el balance inicial si no está configurado
        if (bankProduct.getBalance() == null) {
            bankProduct.setBalance(BigDecimal.ZERO);
        }

        // Validar que tenga al menos un titular
        if (accountHolders == null || accountHolders.isEmpty()) {
            log.error("Bank product must have at least one account holder.");
            return Mono.just(ResponseEntity.badRequest()
                    .body(new Response<>("Bank product must have at least one account holder.", null)));
        }

        // Obtener IDs únicos de los titulares
        List<String> customerIds = accountHolders.stream()
                .map(AccountHolder::getCustomerId)
                .distinct()
                .toList();

        // Validar que todos los titulares
        Flux<Customer> accountCustomerHolders = customerRepository.findByIdIn(customerIds);

        // Consultar detalles de productos existentes
        // Reglas para clientes personales
        // Reglas para clientes empresariales (solo cuentas corrientes)
        // Si pasa las validaciones, guardar el producto

        return accountCustomerHolders
                .collectList()
                .flatMap(customers -> {
                    boolean isAllPersonal = customers.stream()
                            .allMatch(customer -> "Personal".equalsIgnoreCase(customer.getCustomerType()));
                    boolean isAllBusiness = customers.stream()
                            .allMatch(customer -> "Business".equalsIgnoreCase(customer.getCustomerType()));

                    if (!isAllPersonal && !isAllBusiness) {
                        log.error("All account holders must be of the same type: personal or business.");
                        return Mono.just(ResponseEntity.badRequest()
                                .body(new Response<>("All account holders must be of the same type: personal or business.", null)));

                    }

                    // Consultar detalles de productos existentes
                    return Flux.fromIterable(bankProduct.getAccountHolders())
                            .flatMap(holder -> bankProductRepository.findAllByCustomerId(holder.getCustomerId()))
                            .flatMap(existingProduct -> productTypeRepository.findById(existingProduct.getTypeProductId()))
                            .collectList()
                            .flatMap(productDetails -> {
                                boolean isValid;

                                if (isAllPersonal) {
                                    // Reglas para clientes personales
                                    long savingAccounts = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getSaving())).count();
                                    long currentAccounts = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getCurrent())).count();
                                    long fixedAccounts = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getFixed())).count();
                                    long credits = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getCredit())).count();

                                    isValid = savingAccounts <= 1 && currentAccounts <= 1 && fixedAccounts <= 1 && credits <= 1;
                                } else {
                                    // Reglas para clientes empresariales (solo cuentas corrientes)
                                    boolean hasInvalidAccount = productDetails.stream()
                                            .anyMatch(p -> !p.getName().equalsIgnoreCase(productTypeConfig.getCurrent()));
                                    isValid = !hasInvalidAccount;
                                }

                                if (!isValid) {
                                    log.error("Validation failed for the bank product.");
                                    return Mono.just(ResponseEntity.badRequest()
                                            .body(new Response<>("Validation failed for the bank product.", null)));

                                }

                                // Si pasa las validaciones, guardar el producto
                                return bankProductRepository.save(bankProduct)
                                        .doOnSuccess(savedBankProduct -> log.info("Bank product created successfully with ID: {}", savedBankProduct.getId()))
                                        .doOnError(error -> log.error("Error occurred while creating bank product: {}", error.getMessage()))
                                        .map(savedBankProduct -> ResponseEntity.status(HttpStatus.CREATED)
                                                .body(new Response<>("Bank product created successfully.", savedBankProduct)))
                                        .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(new Response<>("An error occurred while creating the bank product.", null))));
                            });
                });
    }

    @Override
    public Mono<ResponseEntity<Response<BankProduct>>> makeTransaction(String productId, Transaction transaction) {
        return bankProductRepository.findById(productId)
                .flatMap(bankProduct -> {
                    BigDecimal currentBalance = bankProduct.getBalance();
                    BigDecimal transactionAmount = transaction.getAmount();

                    // Determinar si la transacción es un retiro (subtract)
                    if (transaction.getSubstract()) {
                        transactionAmount = transactionAmount.negate(); // Cambiar el monto a negativo para un retiro

                        // Validar si hay suficiente balance para realizar el retiro
                        if (currentBalance.add(transactionAmount).compareTo(BigDecimal.ZERO) < 0) {
                            log.error("Insufficient funds for transaction on product ID: {}", productId);
                            // Devuelve un error 400 si no hay suficiente balance
                            return Mono.just(ResponseEntity.badRequest()
                                    .body(new Response<>("Insufficient funds for transaction on product ID: " + productId, new BankProduct())));
                        }
                    }
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    transaction.setDate(dateFormat.format(new Date()));
                    // Actualizar el balance del producto
                    bankProduct.setBalance(currentBalance.add(transactionAmount));
                    // Establecer el monto ajustado (positivo o negativo) en la transacción
                    transaction.setAmount(transactionAmount);
                    // Agregar la transacción a la lista
                    bankProduct.getTransactions().add(transaction);
                    // Guardar el producto con la nueva transacción
                    return bankProductRepository.save(bankProduct)
                            .map(savedProduct -> {
                                log.info("Transaction successful for product ID: {}. New balance: {}", savedProduct.getId(), savedProduct.getBalance());
                                return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(new Response<>("Transaction successful for product ID: " + savedProduct.getId() + ". New balance: " + savedProduct.getBalance(), savedProduct));
                            });
                })
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new Response<>("Product not found with product ID: " + productId, new BankProduct()))));
    }

    @Override
    public Mono<ResponseEntity<Response<BigDecimal>>> getProductBalance(String productId) {
        return bankProductRepository.findById(productId)
                .map(BankProduct::getBalance)
                .map(balance -> ResponseEntity.ok(new Response<>("Balance retrieved successfully.", balance))) // Wrap the balance in the Response
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new Response<>("Product not found with product ID: " + productId, null)))); // Null for no data
    }

    @Override
    public Mono<ResponseEntity<Response<List<Transaction>>>> getProductTransactions(String productId) {
        return bankProductRepository.findById(productId)
                .flatMapMany(bankProduct -> Flux.fromIterable(bankProduct.getTransactions()))
                .collectList()
                .map(transactions -> ResponseEntity.ok(new Response<>("Transactions retrieved successfully.", transactions))) // Wrap the transactions in the Response
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new Response<>("No transactions found for product ID: " + productId, null)))); // Null for no data
    }

    /*@Override
    public Mono<ResponseEntity<BankProduct>> updateBankProduct(String productId, BankProduct bankProduct) {
        return bankProductRepository.findById(productId)
                .flatMap(existingProduct -> {
                    existingProduct.setProductNumber(bankProduct.getProductNumber());
                    existingProduct.setTypeProductId(bankProduct.getTypeProductId());
                    existingProduct.setAccountHolders(bankProduct.getAccountHolders());
                    existingProduct.setBalance(bankProduct.getBalance());
                    existingProduct.setActive(bankProduct.isActive());
                    existingProduct.setTransactions(bankProduct.getTransactions());
                    return bankProductRepository.save(existingProduct)
                            .map(updatedProduct -> {
                                log.info("Bank product updated successfully with ID: {}", updatedProduct.getId());
                                return ResponseEntity.ok(updatedProduct);
                            });
                })
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(null)));
    }*/

    @Override
    public Mono<ResponseEntity<Response<Object>>> deleteBankProduct(String productId) {
        return bankProductRepository.findById(productId)
                .flatMap(existingProduct -> bankProductRepository.delete(existingProduct)
                        .then(Mono.just(ResponseEntity
                                .status(HttpStatus.NO_CONTENT)
                                .body(new Response<>("Bank product deleted successfully.", null))))) // Success message with no data
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new Response<>("Bank product not found with product ID: " + productId, null)))); // Error message with no data
    }

    @Override
    public Flux<ResponseEntity<Response<BankProduct>>> getAllBankProducts() {
        return bankProductRepository.findAll()
                .map(bankProduct -> ResponseEntity.ok(new Response<>("Bank products retrieved successfully", bankProduct))) // Mapear cada producto a ResponseEntity con mensaje de éxito
                .switchIfEmpty(Flux.just(ResponseEntity
                        .status(HttpStatus.NO_CONTENT)
                        .body(new Response<>("No bank products found.", null)))); // En caso de que no haya productos, devolver 204 con mensaje
    }

    @Override
    public Mono<ResponseEntity<Response<BankProduct>>> getBankProductById(String productId) {
        return bankProductRepository.findById(productId)
                .map(bankProduct -> ResponseEntity.ok(new Response<>("Bank product found.", bankProduct))) // Si el producto existe, devolver 200 con mensaje y producto
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new Response<>("Bank product not found with product ID: " + productId, null)))); // Si el producto no se encuentra, devolver 404 con mensaje
    }

}
