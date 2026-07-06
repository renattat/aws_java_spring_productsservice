package renat.aws.productservice.dto;

import renat.aws.productservice.products.models.Product;

public record ProductDto(
        String id,
        String name,
        String code,
        float price,
        String model
) {

    public ProductDto(Product product) {
        this(product.getId(), product.getProductName(), product.getCode(), product.getPrice(), product.getModel());
    }

    static public Product toProduct(ProductDto productDto) {
        Product product = new Product();
        product.setId(productDto.id());
        product.setProductName(productDto.name());
        product.setCode(productDto.code());
        product.setModel(productDto.model());
        product.setPrice(productDto.price());
        return product;
    }

}
