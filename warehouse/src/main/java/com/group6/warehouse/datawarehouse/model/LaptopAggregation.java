package com.group6.warehouse.datawarehouse.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "laptop_aggregation")
public class LaptopAggregation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "total_products", nullable = false)
    private Integer totalProducts;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "average_price", nullable = false)
    private Integer averagePrice;

    @Column(name = "total_stock_qty", nullable = false)
    private Integer totalStockQty;

    @Column(name = "average_discount", nullable = false)
    private Integer averageDiscount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Getters and Setters
}
