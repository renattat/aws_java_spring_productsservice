package renat.aws.productservice.products.controllers;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import renat.aws.productservice.dto.ProductDto;
import renat.aws.productservice.products.models.Product;
import renat.aws.productservice.products.repositories.ProductsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@RestController
@RequestMapping("/api/products")
@XRayEnabled
public class ProductController {

    private final ProductsRepository productsRepository;

    @Autowired
    public ProductController(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    private static final Logger LOG = LogManager.getLogger(ProductController.class);

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        LOG.info("Get all products");
        List<ProductDto> productDto = new ArrayList<>();
        productsRepository.getAll().items().subscribe(product -> {
            productDto.add(new ProductDto(product));
        }).join();

        return new ResponseEntity<>(productDto, HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getProductById(@PathVariable("id") String id) {
        Product product = productsRepository.getById(id).join();
        if (product != null) {
            return new ResponseEntity<>(new ProductDto(product), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
        Product productCreated = ProductDto.toProduct(productDto);

        productCreated.setId(UUID.randomUUID().toString());
        productsRepository.create(productCreated).join();
        LOG.info("Product  created - ID: {}", productCreated.getId());
        return new ResponseEntity<>(new ProductDto(productCreated), HttpStatus.CREATED);
    }

    // DELETE /products/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteProductById(@PathVariable("id") String id) {
        Product productDeleted = productsRepository.deleteById(id).join();
        if (productDeleted != null) {
            LOG.info("Product deleted 0 ID: {}", productDeleted.getId());
            return new ResponseEntity<>(new ProductDto(productDeleted), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<?> updateProduct(@RequestBody ProductDto productDto,
                                           @PathVariable("id") String id) {
        try {
            Product productUpdated = productsRepository
                    .update(ProductDto.toProduct(productDto), id).join();
            LOG.info("Product updated - ID:{}", productUpdated.getId());
            return new ResponseEntity<>(new ProductDto(productUpdated), HttpStatus.OK);
        } catch (CompletionException e) {
            return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
        }


    }

}