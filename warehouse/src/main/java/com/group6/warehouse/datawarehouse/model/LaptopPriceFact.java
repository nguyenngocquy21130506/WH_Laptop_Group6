package com.group6.warehouse.datawarehouse.model;

import javax.persistence.*;

@Entity
@Table(name = "laptop_price_fact")
public class LaptopPriceFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "date_id")
    private Integer dateId;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "brand_id")
    private Integer brandId;

    // Getters and Setters
}
