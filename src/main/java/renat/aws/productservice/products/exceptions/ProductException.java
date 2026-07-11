package renat.aws.productservice.products.exceptions;

import org.springframework.lang.Nullable;
import renat.aws.productservice.products.enums.ProductErrors;

public class ProductException extends Exception {

    private final ProductErrors productErrors;
    @Nullable
    private final String productId;

    public ProductException(ProductErrors productErrors, @Nullable String productId) {
        this.productErrors = productErrors;
        this.productId = productId;
    }

    public ProductErrors getProductErrors() {
        return productErrors;
    }

    @Nullable
    public String getProductId() {
        return productId;
    }
}
