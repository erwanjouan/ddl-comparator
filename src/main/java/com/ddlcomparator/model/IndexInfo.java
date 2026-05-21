package com.ddlcomparator.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class IndexInfo {
    private final String name;
    private final String type;
    private final List<String> columns;

    public IndexInfo(String name, String type, List<String> columns) {
        this.name = name;
        this.type = type;
        this.columns = columns;
    }

    public String name() { return name; }
    public String type() { return type; }
    public List<String> columns() { return columns; }

    public String structuralKey() {
        return type.toUpperCase() + ":"
                + columns.stream().map(String::toLowerCase).sorted().collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndexInfo)) return false;
        IndexInfo that = (IndexInfo) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(columns, that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, columns);
    }

    @Override
    public String toString() {
        return "IndexInfo[name=" + name + ", type=" + type + "]";
    }
}
