package renat.aws.productservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import renat.aws.productservice.products.interceptors.ProductsInterceptor;

@Configuration
public class InterceptorsConfig implements WebMvcConfigurer {

    private final ProductsInterceptor productsInterceptor;

    @Autowired
    public InterceptorsConfig(ProductsInterceptor productsInterceptor) {
        this.productsInterceptor = productsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.productsInterceptor)
                .addPathPatterns("/api/products/**");
    }
}
