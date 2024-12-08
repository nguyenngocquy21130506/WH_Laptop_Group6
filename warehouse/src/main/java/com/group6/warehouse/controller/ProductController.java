package com.group6.warehouse.controller;

import com.group6.warehouse.dao.ProductDAO;
import com.group6.warehouse.model.Product;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    private ProductDAO productDAO = new ProductDAO();
    @PostMapping("/productsToDay/{index}")
    public List<Product> getProductToDay(@PathVariable int index) {
        return productDAO.getAllProduct(index);
    }

    @PostMapping("/productDetail/{id}")
    public Product getAllProduct(@PathVariable int id) {
        return productDAO.getProductById(id);
    }

    @PostMapping("/searchProducts/{name}")
    public List<Product> searchProduct(@PathVariable String name) {
        return productDAO.searchProduct(name);
    }
}
