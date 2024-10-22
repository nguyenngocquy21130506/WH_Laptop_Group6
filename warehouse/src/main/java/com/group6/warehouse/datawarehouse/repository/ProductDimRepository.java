package com.group6.warehouse.datawarehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.datawarehouse.model.ProductDim;

public interface ProductDimRepository extends JpaRepository<ProductDim, Integer> {
    // Các phương thức tùy chỉnh nếu cần
}
