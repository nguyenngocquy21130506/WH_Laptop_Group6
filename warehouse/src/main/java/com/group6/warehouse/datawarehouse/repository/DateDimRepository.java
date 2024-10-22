package com.group6.warehouse.datawarehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.datawarehouse.model.DateDim;

public interface DateDimRepository extends JpaRepository<DateDim, Integer> {
    // Các phương thức tùy chỉnh nếu cần
}
