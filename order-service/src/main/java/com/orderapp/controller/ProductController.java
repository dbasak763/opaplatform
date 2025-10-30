package com.orderapp.controller;

import com.orderapp.entity.Product;
import com.orderapp.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductRepository productRepository;

    @Autowired
    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Product.ProductStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        logger.info("Fetching products - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> products = productRepository.findProductsWithFilters(
            name, category, brand, status, minPrice, maxPrice, pageable);

        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID productId) {
        logger.info("Fetching product with id: {}", productId);
        return productRepository.findById(productId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        logger.info("Fetching product with SKU: {}", sku);
        return productRepository.findBySku(sku)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Product>> getAvailableProducts() {
        logger.info("Fetching available products");
        List<Product> products = productRepository.findAvailableProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        logger.info("Fetching products for category: {}", category);
        List<Product> products = productRepository.findByCategory(category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<Product>> getProductsByBrand(@PathVariable String brand) {
        logger.info("Fetching products for brand: {}", brand);
        List<Product> products = productRepository.findByBrand(brand);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts(
            @RequestParam(defaultValue = "10") Integer threshold) {
        logger.info("Fetching low stock products with threshold: {}", threshold);
        List<Product> products = productRepository.findLowStockProducts(threshold);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        logger.info("Fetching all product categories");
        List<String> categories = productRepository.findAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        logger.info("Fetching all product brands");
        List<String> brands = productRepository.findAllBrands();
        return ResponseEntity.ok(brands);
    }
}
