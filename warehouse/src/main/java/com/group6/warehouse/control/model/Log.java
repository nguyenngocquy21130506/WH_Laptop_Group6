package com.group6.warehouse.control.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "logs")
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event", length = 100, nullable = false)
    private String event;

    @Column(name = "status", length = 255, nullable = false)
    private String status;

    @Column(name = "note", length = 1000)
    private String note;

    // Đổi tên cột thành created_at cho đồng bộ
    @Column(name = "created_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    // Getters and Setters
    // Constructor (default và có tham số)
}
