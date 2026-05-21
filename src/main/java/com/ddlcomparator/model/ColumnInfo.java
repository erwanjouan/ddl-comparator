package com.ddlcomparator.model;

import java.util.List;
import java.util.Objects;

public final class ColumnInfo {
    private final String name;
    private final String dataType;
    private final boolean nullable;
    private final String defaultValue;
    private final List<String> columnSpecs;

    public ColumnInfo(String name, String dataType, boolean nullable, String defaultValue, List<String> columnSpecs) {
        this.name = name;
        this.dataType = dataType;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.columnSpecs = columnSpecs;
    }

    public String name() { return name; }
    public String dataType() { return dataType; }
    public boolean nullable() { return nullable; }
    public String defaultValue() { return defaultValue; }
    public List<String> columnSpecs() { return columnSpecs; }

    public String signature() {
        return name.toLowerCase() + ":" + dataType.toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnInfo)) return false;
        ColumnInfo that = (ColumnInfo) o;
        return nullable == that.nullable &&
                Objects.equals(name, that.name) &&
                Objects.equals(dataType, that.dataType) &&
                Objects.equals(defaultValue, that.defaultValue) &&
                Objects.equals(columnSpecs, that.columnSpecs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dataType, nullable, defaultValue, columnSpecs);
    }

    @Override
    public String toString() {
        return "ColumnInfo[name=" + name + ", dataType=" + dataType + ", nullable=" + nullable +
                ", defaultValue=" + defaultValue + ", columnSpecs=" + columnSpecs + "]";
    }
}
