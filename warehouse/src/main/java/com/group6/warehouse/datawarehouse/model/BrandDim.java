package com.group6.warehouse.datawarehouse.model;

import javax.persistence.*;

@Entity
@Table(name = "brand_dim")
public class BrandDim {

    @Id
    private Integer id;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    // Getters and Setters
}
