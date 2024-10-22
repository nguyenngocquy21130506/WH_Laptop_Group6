package com.group6.warehouse.datawarehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.datawarehouse.model.BrandDim;

public interface BrandDimRepository extends JpaRepository<BrandDim, Integer> {
    // Các phương thức tùy chỉnh nếu cần
}
