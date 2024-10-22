package com.group6.warehouse.mart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.mart.model.LaptopPriceSummary;

public interface LaptopPriceSummaryRepository extends JpaRepository<LaptopPriceSummary, Integer> {
    // Các phương thức tùy chỉnh nếu cần
}
