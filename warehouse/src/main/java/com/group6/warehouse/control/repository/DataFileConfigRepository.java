package com.group6.warehouse.control.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.control.model.DataFileConfig;

public interface DataFileConfigRepository extends JpaRepository<DataFileConfig, Long> {
    DataFileConfig findByName(String name);
}
