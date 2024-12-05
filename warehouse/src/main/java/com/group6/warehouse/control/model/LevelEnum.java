package com.group6.warehouse.control.model;

public enum LevelEnum {
    INFO(0),
    ERROR(1);
    private final int value;
    LevelEnum(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
