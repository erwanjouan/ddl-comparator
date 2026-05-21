package com.ddlcomparator.model;

import java.util.List;
import java.util.Objects;

public final class TableInfo {
    private final String name;
    private final List<ColumnInfo> columns;
    private final List<IndexInfo> indexes;
    private final List<ForeignKeyInfo> foreignKeys;

    public TableInfo(String name, List<ColumnInfo> columns, List<IndexInfo> indexes, List<ForeignKeyInfo> foreignKeys) {
        this.name = name;
        this.columns = columns;
        this.indexes = indexes;
        this.foreignKeys = foreignKeys;
    }

    public String name() { return name; }
    public List<ColumnInfo> columns() { return columns; }
    public List<IndexInfo> indexes() { return indexes; }
    public List<ForeignKeyInfo> foreignKeys() { return foreignKeys; }

    public String nameLower() {
        return name.toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableInfo)) return false;
        TableInfo that = (TableInfo) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(columns, that.columns) &&
                Objects.equals(indexes, that.indexes) &&
                Objects.equals(foreignKeys, that.foreignKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns, indexes, foreignKeys);
    }

    @Override
    public String toString() {
        return "TableInfo[name=" + name + "]";
    }
}
