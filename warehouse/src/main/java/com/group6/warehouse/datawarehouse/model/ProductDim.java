package com.group6.warehouse.datawarehouse.model;

import javax.persistence.*;

@Entity
@Table(name = "product_dim")
public class ProductDim {

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

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "inventory_status", length = 20)
    private String inventoryStatus;

    @Column(name = "stock_item_qty")
    private Integer stockItemQty;

    // Getters and Setters
}
