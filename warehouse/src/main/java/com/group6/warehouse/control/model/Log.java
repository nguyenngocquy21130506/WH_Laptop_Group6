package com.group6.warehouse.control.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private LevelEnum level;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "end_time")
    private LocalDateTime endTime;
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.endTime = now; // Nếu bạn muốn thiết lập endTime ngay tại lúc tạo
    }

}