package com.group6.warehouse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class ProductController {

    @PostMapping("/productsToDay")
    public String getProductToDay() {
        // get data from martdb by repository
        return "redirect:/products";
    }

    @PostMapping("/products")
    public String getAllProduct() {
        return "redirect:/products";
    }
}
