package com.group6.warehouse.staging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.staging.model.Laptop;

public interface LaptopRepository extends JpaRepository<Laptop, Integer> {
}
