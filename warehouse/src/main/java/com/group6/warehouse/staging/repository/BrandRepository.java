package com.group6.warehouse.staging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.staging.model.Brand;

public interface BrandRepository extends JpaRepository<Brand, Integer> {
}
