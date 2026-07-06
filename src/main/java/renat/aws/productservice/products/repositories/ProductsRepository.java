package renat.aws.productservice.products.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import renat.aws.productservice.products.models.Product;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.concurrent.CompletableFuture;

@Repository
public class ProductsRepository {

    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    private DynamoDbAsyncTable<Product> productsTable;

    @Autowired
    public ProductsRepository(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                              @Value("${aws.productsddb.name}") String productsDbbName) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productsTable = dynamoDbEnhancedAsyncClient.table(productsDbbName, TableSchema.fromBean(Product.class));

    }

    public PagePublisher<Product> getAll() {
        //DO NOT DO THIS IN PRODUCTION
        return productsTable.scan();
    }

    public CompletableFuture<Product> getById(String productId) {
        return productsTable.getItem(Key.builder()
                    .partitionValue(productId)
                .build());
    }

    public CompletableFuture<Void> create(Product product) {
        return productsTable.putItem(product);
    }

    public CompletableFuture<Product> deleteById(String productId) {
        return productsTable.deleteItem(Key.builder()
                .partitionValue(productId)
                .build());
    }

    public CompletableFuture<Product> update(Product product, String productId) {
        product.setId(productId);
        return productsTable.updateItem(
                UpdateItemEnhancedRequest.builder(Product.class)
                        .item(product)
                        .conditionExpression(Expression.builder()
                                .expression("attribute_exists(id)")
                                .build())
                        .build()
        );

    }



}
