package com.niocess.perflab.client.db;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private String name;
    private BigDecimal price;
    private String currency;

    protected Product() {
    }

    Product(Long id, String sku, String name, BigDecimal price, String currency) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.currency = currency;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
}
