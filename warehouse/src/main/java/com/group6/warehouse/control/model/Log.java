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
    private LevelEnum level;
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;
    @Column(name = "end_time")
    private Timestamp endTime;

    @PrePersist
    public void prePersist() {
        // Lấy thời gian hiện tại
        long currentTimeMillis = System.currentTimeMillis();

        // Tạo Calendar và cộng thêm 7 giờ
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis);
        calendar.add(Calendar.HOUR, 7);  // Thêm 7 giờ

        // Lưu giá trị vào các trường
        createdAt = new Timestamp(calendar.getTimeInMillis());
        endTime = new Timestamp(calendar.getTimeInMillis());
    }
}