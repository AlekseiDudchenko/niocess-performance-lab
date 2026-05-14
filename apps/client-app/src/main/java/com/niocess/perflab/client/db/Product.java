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

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }

    void setId(Long id) { this.id = id; }
    void setSku(String sku) { this.sku = sku; }
    void setName(String name) { this.name = name; }
    void setPrice(BigDecimal price) { this.price = price; }
    void setCurrency(String currency) { this.currency = currency; }
}
