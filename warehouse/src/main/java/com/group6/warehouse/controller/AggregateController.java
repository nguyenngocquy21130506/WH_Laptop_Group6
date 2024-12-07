package com.group6.warehouse.controller;

import com.group6.warehouse.dao.ProductDAO;
import com.group6.warehouse.model.Aggregate;
import com.group6.warehouse.model.Product;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/overview")
public class AggregateController {

    private ProductDAO productDAO = new ProductDAO();

    @PostMapping("/overview")
    public Aggregate getAggregate() {
        return productDAO.getAggregate();
    }
}
