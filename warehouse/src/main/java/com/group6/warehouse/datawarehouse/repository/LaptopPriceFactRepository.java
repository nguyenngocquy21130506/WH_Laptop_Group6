package com.group6.warehouse.datawarehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.datawarehouse.model.LaptopPriceFact;

public interface LaptopPriceFactRepository extends JpaRepository<LaptopPriceFact, Integer> {
    // Các phương thức tùy chỉnh nếu cần
}
