package com.group6.warehouse;

public enum LevelEnum {
    INFO(0),
    WARNING(1),
    ERROR(2);

    private final int levelCode;

    LevelEnum(int levelCode) {
        this.levelCode = levelCode;
    }

    public int getLevelCode() {
        return levelCode;
    }

    public static LevelEnum fromInt(int i) {
        switch (i) {
            case 0: return INFO;
            case 1: return WARNING;
            case 2: return ERROR;
            default: throw new IllegalArgumentException("Unexpected value: " + i);
        }
    }
}
