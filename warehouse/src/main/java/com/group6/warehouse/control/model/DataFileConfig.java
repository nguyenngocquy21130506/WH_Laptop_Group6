package com.group6.warehouse.control.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "data_file_configs")
public class DataFileConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 500, nullable = false)
    private String name;

    @Column(name = "name_table", length = 255, nullable = false)
    private String nameTable;

    @Column(name = "filename", length = 255, nullable = false)
    private String filename;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_path", length = 1000)
    private String sourcePath;

    @Column(name = "directory_file", length = 1000)
    private String directoryFile;

    @Column(name = "format", length = 255)
    private String format;

    @Column(name = "columns", length = 255)
    private String columns;

    @Column(name = "extraction_frequency")
    private int extractionFrequency;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and Setters
    // Constructor (default và có tham số)
}
