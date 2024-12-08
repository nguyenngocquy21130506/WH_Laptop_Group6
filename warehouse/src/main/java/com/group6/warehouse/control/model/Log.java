package com.group6.warehouse.control.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "logs")
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "id_config", nullable = false)
    private Long idConfig;
    @Column(name = "task_name", length = 100, nullable = false)
    private String taskName;
    @Column(name = "status", length = 255, nullable = false)
    private String status;
    @Column(name = "message", length = 1000)
    private String message;
    @Column(name = "level", length = 100)
    private int level;


}