package com.group6.warehouse.staging.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "laptops")
public class Laptop {

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

    @Column(name = "discount_rate")
    private Integer discountRate;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "order_count")
    private Integer orderCount;

    @Column(name = "inventory_status", length = 20)
    private String inventoryStatus;

    @Column(name = "stock_item_qty")
    private Integer stockItemQty;

    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
}
