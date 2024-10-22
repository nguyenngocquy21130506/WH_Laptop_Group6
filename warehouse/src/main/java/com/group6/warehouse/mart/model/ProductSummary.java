package com.group6.warehouse.mart.model;

import javax.persistence.*;

@Entity
@Table(name = "product_summary")
public class ProductSummary {

    @Id
    private Integer id;

    @Column(name = "sku", length = 20)
    private String sku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "short_description", length = 255)
    private String shortDescription;

    @Column(name = "price")
    private Integer price;

    @Column(name = "discount")
    private Integer discount;

    @Column(name = "discount_rate", length = 5)
    private Integer discountRate;

    @Column(name = "review_count", length = 10)
    private Integer reviewCount;

    @Column(name = "inventory_status", length = 20)
    private String inventoryStatus;

    @Column(name = "stock_item_qty", length = 10)
    private Integer stockItemQty;

    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    // Getters and Setters
}
