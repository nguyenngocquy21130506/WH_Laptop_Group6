package com.group6.warehouse.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Aggregate {
    private int id;
    private int avg_price;
    private int min_price;
    private int max_price;
    private String min_price_product_name;
    private String max_price_product_name;
    private String total_products;
}
