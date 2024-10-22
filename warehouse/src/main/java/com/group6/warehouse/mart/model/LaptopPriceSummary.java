package com.group6.warehouse.mart.model;

import javax.persistence.*;

@Entity
@Table(name = "laptop_price_summary")
public class LaptopPriceSummary {

    @Id
    private Integer id;

    @Column(name = "sku", length = 20)
    private String sku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "price")
    private Integer price;

    @Column(name = "discount_rate", length = 5)
    private Integer discountRate;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    // Getters and Setters
}
