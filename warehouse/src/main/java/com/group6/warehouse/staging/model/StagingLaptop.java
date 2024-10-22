package com.group6.warehouse.staging.model;

import javax.persistence.*;

@Entity
@Table(name = "staging_laptop")
public class StagingLaptop {

    @Id
    private Integer id;

    @Column(name = "sku", length = 255)
    private String sku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "short_description", length = 1000)
    private String shortDescription;

    @Column(name = "price", length = 255)
    private String price;

    @Column(name = "discount", length = 255)
    private String discount;

    @Column(name = "discount_rate", length = 255)
    private String discountRate;

    @Column(name = "review_count", length = 255)
    private String reviewCount;

    @Column(name = "order_count", length = 255)
    private String orderCount;

    @Column(name = "inventory_status", length = 255)
    private String inventoryStatus;

    @Column(name = "stock_item_qty", length = 255)
    private String stockItemQty;

    @Column(name = "brand_id", length = 255)
    private String brandId;

    @Column(name = "created_at", length = 255)
    private String createdAt;

    @Column(name = "updated_at", length = 255)
    private String updatedAt;

    // Getters and Setters
}
