package com.group6.warehouse.control.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.group6.warehouse.control.model.Log;

public interface LogRepository extends JpaRepository<Log, Long> {
}
