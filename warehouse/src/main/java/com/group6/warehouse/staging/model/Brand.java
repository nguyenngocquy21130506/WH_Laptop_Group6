package com.group6.warehouse.staging.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "brand")
public class Brand {

    @Id
    private Integer id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters and Setters
}
