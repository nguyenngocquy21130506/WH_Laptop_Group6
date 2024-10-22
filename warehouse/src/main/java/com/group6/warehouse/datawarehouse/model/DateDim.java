package com.group6.warehouse.datawarehouse.model;

import javax.persistence.*;

@Entity
@Table(name = "date_dim")
public class DateDim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_date")
    private java.sql.Date fullDate;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "month")
    private Integer month;

    @Column(name = "year")
    private Integer year;

    // Getters and Setters
}
