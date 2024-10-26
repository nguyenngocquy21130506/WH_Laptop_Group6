package com.group6.warehouse.mart.model;

import javax.persistence.*;

@Entity
@Table(name = "time_summary")
public class TimeSummary {

    @Id
    private Integer id;

    @Column(name = "full_date")
    private java.sql.Date fullDate;

    // Getters and Setters
}
