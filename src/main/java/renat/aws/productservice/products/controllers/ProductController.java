package renat.aws.productservice.products.controllers;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import renat.aws.productservice.events.dto.EventType;
import renat.aws.productservice.events.service.EventsPublisher;
import renat.aws.productservice.products.dto.ProductDto;
import renat.aws.productservice.products.enums.ProductErrors;
import renat.aws.productservice.products.exceptions.ProductException;
import renat.aws.productservice.products.models.Product;
import renat.aws.productservice.products.repositories.ProductsRepository;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/products")
@XRayEnabled
public class ProductController {

    private final ProductsRepository productsRepository;
    private final EventsPublisher eventsPublisher;

    @Autowired
    public ProductController(ProductsRepository productsRepository, EventsPublisher eventsPublisher) {
        this.productsRepository = productsRepository;
        this.eventsPublisher = eventsPublisher;
    }

    private static final Logger LOG = LogManager.getLogger(ProductController.class);

    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false) String code) throws ProductException {
        if (code != null) {
            LOG.info("Get product by cod: {}", code);
            Product productByCode = productsRepository.getByCode(code).join();
            if (productByCode != null) {
                return new ResponseEntity<>(new ProductDto(productByCode), HttpStatus.OK);
            } else {
                throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, null);
            }
        } else {
            LOG.info("Get all products");
            List<ProductDto> productDto = new ArrayList<>();
            productsRepository.getAll().items().subscribe(product -> {
                productDto.add(new ProductDto(product));
            }).join();

            return new ResponseEntity<>(productDto, HttpStatus.OK);
        }

    }

    @GetMapping("{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("id") String id) throws ProductException {
        Product product = productsRepository.getById(id).join();
        if (product != null) {
            LOG.info("Get product by its id: {}", id);
            return new ResponseEntity<>(new ProductDto(product), HttpStatus.OK);
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) throws ProductException, JsonProcessingException, ExecutionException, InterruptedException {
        Product productCreated = ProductDto.toProduct(productDto);
        productCreated.setId(UUID.randomUUID().toString());
        // we do in parallel productsRepository.create() and eventsPublisher.sendProductEvent()
        CompletableFuture<Void> productCompletableFeature = productsRepository.create(productCreated);
        CompletableFuture<PublishResponse> publishResponseCompletableFuture = eventsPublisher.sendProductEvent(productCreated, EventType.PRODUCT_CREATED, "roman.tat@mail.ru");

        // the point where we wait to both operations are completed
        CompletableFuture.allOf(productCompletableFeature, publishResponseCompletableFuture).join();
        PublishResponse publishResponse = publishResponseCompletableFuture.get();
        ThreadContext.put("messageId", publishResponse.messageId());
        LOG.info("Product  created - ID: {}", productCreated.getId());
        return new ResponseEntity<>(new ProductDto(productCreated), HttpStatus.CREATED);
    }

    // DELETE /products/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<ProductDto> deleteProductById(@PathVariable("id") String id) throws ProductException, JsonProcessingException {
        Product productDeleted = productsRepository.deleteById(id).join();
        if (productDeleted != null) {
            PublishResponse publishResponse = eventsPublisher.sendProductEvent(productDeleted, EventType.PRODUCT_DELETED, "ruslan.tat@mail.ru").join();
            ThreadContext.put("messageId", publishResponse.messageId());
            LOG.info("Product deleted 0 ID: {}", productDeleted.getId());
            return new ResponseEntity<>(new ProductDto(productDeleted), HttpStatus.OK);
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductDto> updateProduct(@RequestBody ProductDto productDto,
                                                    @PathVariable("id") String id) throws ProductException, JsonProcessingException {
        try {
            Product productUpdated = productsRepository
                    .update(ProductDto.toProduct(productDto), id).join();
            PublishResponse publishResponse = eventsPublisher.sendProductEvent(productUpdated, EventType.PRODUCT_UPDATED, "raden.tat@mail.ru").join();
            ThreadContext.put("messageId", publishResponse.messageId());
            LOG.info("Product updated - ID:{}", productUpdated.getId());
            return new ResponseEntity<>(new ProductDto(productUpdated), HttpStatus.OK);
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }


    }

}