package com.nttdata.bootcamp_bank_product_service.service.impl;

import com.nttdata.bootcamp_bank_product_service.client.CustomerClient;
import com.nttdata.bootcamp_bank_product_service.client.ProductTypeClient;
import com.nttdata.bootcamp_bank_product_service.config.ProductTypeConfig;
import com.nttdata.bootcamp_bank_product_service.model.collection.BankProduct;
import com.nttdata.bootcamp_bank_product_service.model.dto.Customer;
import com.nttdata.bootcamp_bank_product_service.model.dto.Transaction;
import com.nttdata.bootcamp_bank_product_service.model.dto.AccountHolder;
import com.nttdata.bootcamp_bank_product_service.model.response.Response;
import com.nttdata.bootcamp_bank_product_service.repository.BankProductRepository;
import com.nttdata.bootcamp_bank_product_service.service.BankProductService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
@Service
public class BankProductServiceImpl implements BankProductService {

    private final BankProductRepository bankProductRepository;
    private final ProductTypeClient productTypeClient;
    private final CustomerClient customerClient;
    private final ProductTypeConfig productTypeConfig;

    public BankProductServiceImpl(
            BankProductRepository bankProductRepository,
            ProductTypeClient productTypeClient,
            CustomerClient customerClient,
            ProductTypeConfig productTypeConfig) {
        this.bankProductRepository = bankProductRepository;
        this.productTypeClient = productTypeClient;
        this.customerClient = customerClient;
        this.productTypeConfig = productTypeConfig;
    }

    @Override
    public Mono<ResponseEntity<Response<BankProduct>>> createBankProduct(BankProduct bankProduct, String bearerToken) {
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
        Flux<Customer> accountCustomerHolders = customerClient.findByIdIn(customerIds, bearerToken);

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

                    return Flux.fromIterable(bankProduct.getAccountHolders())
                            .flatMap(holder -> bankProductRepository.findAllByCustomerId(holder.getCustomerId()))
                            .flatMap(existingProduct -> productTypeClient.findById(existingProduct.getTypeProductId(), bearerToken))
                            .collectList()
                            .flatMap(productDetails -> {
                                boolean isValid;

                                if (isAllPersonal) {
                                    long savingAccounts = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getSaving())).count();
                                    long currentAccounts = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getCurrent())).count();
                                    long fixedAccounts = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getFixed())).count();
                                    long credits = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getCredit())).count();
                                    long vipAccounts = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getSavingVip())).count();

                                    // Reglas para clientes personales
                                    isValid = savingAccounts <= 1 && currentAccounts <= 1 && fixedAccounts <= 1 && credits <= 1 && vipAccounts <= 1;

                                    // Validar requisitos para cuenta VIP
                                    if (bankProduct.getTypeProductId().equalsIgnoreCase(productTypeConfig.getSavingVip())) {
                                        boolean hasCreditCard = productDetails.stream()
                                                .anyMatch(p -> p.getName().equalsIgnoreCase(productTypeConfig.getCredit()));
                                        if (!hasCreditCard) {
                                            log.error("A VIP account requires the customer to have a credit card.");
                                            return Mono.just(ResponseEntity.badRequest()
                                                    .body(new Response<>("A VIP account requires the customer to have a credit card.", null)));
                                        }
                                    }
                                } else {
                                    boolean hasInvalidAccount = productDetails.stream()
                                            .anyMatch(p -> !p.getName().equalsIgnoreCase(productTypeConfig.getCurrent()));
                                    long pymeAccounts = productDetails.stream()
                                            .filter(p -> p.getName().equalsIgnoreCase(productTypeConfig.getCurrentMype())).count();

                                    // Reglas para clientes empresariales (solo cuentas corrientes y PYME)
                                    isValid = !hasInvalidAccount && pymeAccounts <= 1;

                                    // Validar requisitos para cuenta PYME
                                    if (bankProduct.getTypeProductId().equalsIgnoreCase(productTypeConfig.getCurrentMype())) {
                                        boolean hasCreditCard = productDetails.stream()
                                                .anyMatch(p -> p.getName().equalsIgnoreCase(productTypeConfig.getCredit()));
                                        if (!hasCreditCard) {
                                            log.error("A PYME account requires the customer to have a credit card.");
                                            return Mono.just(ResponseEntity.badRequest()
                                                    .body(new Response<>("A PYME account requires the customer to have a credit card.", null)));
                                        }
                                    }
                                }

                                if (!isValid) {
                                    log.error("Validation failed for the bank product.");
                                    return Mono.just(ResponseEntity.badRequest()
                                            .body(new Response<>("Validation failed for the bank product.", null)));
                                }
                                return productTypeClient.findById(bankProduct.getTypeProductId(), bearerToken)
                                        .flatMap(productType -> {
                                            if (bankProduct.getBalance().compareTo(productType.getAmount()) <= 0) {
                                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                Transaction initialTransaction = new Transaction();
                                                initialTransaction.setProductId(bankProduct.getId());
                                                initialTransaction.setSubstract(false);
                                                initialTransaction.setAmount(bankProduct.getBalance());
                                                initialTransaction.setDate(dateFormat.format(new Date()));
                                                initialTransaction.setDescription("New Account creation.");
                                                bankProduct.getTransactions().add(initialTransaction);
                                            }

                                            return bankProductRepository.save(bankProduct)
                                                    .doOnSuccess(savedBankProduct -> log.info("Bank product created successfully with ID: {}", savedBankProduct.getId()))
                                                    .doOnError(error -> log.error("Error occurred while creating bank product: {}", error.getMessage()))
                                                    .map(savedBankProduct -> ResponseEntity.status(HttpStatus.CREATED)
                                                            .body(new Response<>("Bank product created successfully.", savedBankProduct)))
                                                    .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                            .body(new Response<>("An error occurred while creating the bank product.", null))));
                                        });
                            });
                });
    }

    @Override
    public Mono<ResponseEntity<Response<BankProduct>>> makeTransaction(String productId, Transaction transaction) {
        return bankProductRepository.findById(productId)
                .flatMap(bankProduct ->
                        productTypeClient.findById(bankProduct.getTypeProductId(), "Bearer Token")
                                .flatMap(productType -> {
                                    BigDecimal currentBalance = bankProduct.getBalance();
                                    BigDecimal transactionAmount = transaction.getAmount();
                                    boolean isWithdrawal = transaction.getSubstract();
                                    if (isWithdrawal) {
                                        transactionAmount = transactionAmount.negate();
                                    }
                                    boolean isCredit = productType.getName().equalsIgnoreCase(productTypeConfig.getCredit());

                                    // Validar que el monto para créditos sea siempre negativo
                                    if (isCredit && isWithdrawal) {
                                        log.error("Transactions for credit products must have a negative amount. Product ID: {}", productId);
                                        return Mono.just(ResponseEntity.badRequest()
                                                .body(new Response<>("Transactions for credit products must have a negative amount. Product ID: " + productId, new BankProduct())));
                                    }

                                    // Contar las transacciones del mes
                                    long currentMonthTransactions = bankProduct.getTransactions().stream()
                                            .filter(t -> isTransactionInCurrentMonth(t.getDate()))
                                            .count();

                                    BigDecimal commission = BigDecimal.ZERO;
                                    if (currentMonthTransactions >= productType.getTransactionCount()) {
                                        commission = productType.getCommission().negate(); // La comisión ya es negativa
                                    }

                                    // Calcular el monto total para deducir (retiro + comisión si aplica)
                                    BigDecimal totalTransaction = transactionAmount.add(commission);
                                    if ((isCredit && currentBalance.add(totalTransaction).compareTo(BigDecimal.ZERO) > 0) ||
                                            (!isCredit && currentBalance.add(totalTransaction).compareTo(BigDecimal.ZERO) < 0)) {
                                        String errorMessage = isCredit ? "The transaction amount exceeds the outstanding debt for product ID: " + productId :
                                                "Insufficient funds for transaction (including commission) on product ID: " + productId;
                                        log.error(errorMessage);
                                        return Mono.just(ResponseEntity.badRequest()
                                                .body(new Response<>(errorMessage, new BankProduct())));
                                    }
                                    // Aplicar deducción del monto de retiro y comisión
                                    bankProduct.setBalance(currentBalance.add(totalTransaction));

                                    // Agregar la transacción al historial
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    transaction.setDate(dateFormat.format(new Date()));
                                    bankProduct.getTransactions().add(transaction);

                                    // Si se aplicó comisión, registrar una transacción separada para la comisión
                                    if (commission.compareTo(BigDecimal.ZERO) < 0) {
                                        Transaction commissionTransaction = new Transaction();
                                        commissionTransaction.setProductId(productId);
                                        commissionTransaction.setSubstract(true);
                                        commissionTransaction.setAmount(commission);
                                        commissionTransaction.setDate(dateFormat.format(new Date()));
                                        commissionTransaction.setDescription("Commission fee for exceeding free transactions");
                                        bankProduct.getTransactions().add(commissionTransaction);
                                    }

                                    // Cambiar el estado a inactivo si el saldo del crédito es 0 o menor
                                    if (isCredit && bankProduct.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                                        bankProduct.setActive(false);
                                        log.info("Credit product ID: {} is now inactive due to zero or negative balance.", productId);
                                    }

                                    // Guardar el producto bancario con las transacciones actualizadas
                                    return bankProductRepository.save(bankProduct)
                                            .map(savedProduct -> {
                                                log.info("Transaction successful for product ID: {}. New balance: {}", savedProduct.getId(), savedProduct.getBalance());
                                                return ResponseEntity.status(HttpStatus.CREATED)
                                                        .body(new Response<>("Transaction successful for product ID: " +
                                                                savedProduct.getId() + ". New balance: " +
                                                                savedProduct.getBalance(), savedProduct));
                                            });
                                })
                )
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new Response<>("Product not found with product ID: " + productId, new BankProduct()))));
    }

    private boolean isTransactionInCurrentMonth(String date) {
        try {
            LocalDate transactionDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDate now = LocalDate.now();
            return transactionDate.getYear() == now.getYear() && transactionDate.getMonth() == now.getMonth();
        } catch (DateTimeParseException e) {
            log.error("Error parsing transaction date: {}", date);
            return false;
        }
    }


    @Override
    public Mono<ResponseEntity<Response<BigDecimal>>> getProductBalance(String productId) {
        return bankProductRepository.findById(productId)
                .map(BankProduct::getBalance)
                .map(balance -> ResponseEntity.ok(new Response<>("Balance retrieved successfully.", balance))) // Wrap the balance in the Response
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new Response<>("Product not found with product ID: " + productId, null))));
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
                .map(bankProduct -> ResponseEntity.ok(new Response<>("Bank product retrieved successfully", bankProduct)))
                .defaultIfEmpty(ResponseEntity
                        .status(HttpStatus.NO_CONTENT)
                        .body(new Response<>("No bank products found.", null))); // Usar defaultIfEmpty para un solo elemento vacío
    }

    @Override
    public Mono<ResponseEntity<Response<BankProduct>>> getBankProductById(String productId) {
        return bankProductRepository.findById(productId)
                .map(bankProduct -> ResponseEntity.ok(new Response<>("Success .", bankProduct))) // Si el producto existe, devolver 200 con mensaje y producto
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new Response<>("Bank product not found with product ID: " + productId, null)))); // Si el producto no se encuentra, devolver 404 con mensaje
    }

}
