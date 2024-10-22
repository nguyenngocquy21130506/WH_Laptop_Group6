package com.group6.warehouse.mart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.mart.model.ProductSummary;

public interface ProductSummaryRepository extends JpaRepository<ProductSummary, Integer> {
    // Các phương thức tùy chỉnh nếu cần
}
