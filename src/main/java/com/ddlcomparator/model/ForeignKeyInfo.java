package com.ddlcomparator.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ForeignKeyInfo {
    private final String name;
    private final List<String> localColumns;
    private final String referencedTable;
    private final List<String> referencedColumns;
    private final String onDelete;
    private final String onUpdate;

    public ForeignKeyInfo(String name, List<String> localColumns, String referencedTable,
                          List<String> referencedColumns, String onDelete, String onUpdate) {
        this.name = name;
        this.localColumns = localColumns;
        this.referencedTable = referencedTable;
        this.referencedColumns = referencedColumns;
        this.onDelete = onDelete;
        this.onUpdate = onUpdate;
    }

    public String name() { return name; }
    public List<String> localColumns() { return localColumns; }
    public String referencedTable() { return referencedTable; }
    public List<String> referencedColumns() { return referencedColumns; }
    public String onDelete() { return onDelete; }
    public String onUpdate() { return onUpdate; }

    public String structuralKey() {
        return localColumns.stream().map(String::toLowerCase).sorted().collect(Collectors.joining(","))
                + "->" + referencedTable.toLowerCase()
                + "(" + referencedColumns.stream().map(String::toLowerCase).sorted().collect(Collectors.joining(",")) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForeignKeyInfo)) return false;
        ForeignKeyInfo that = (ForeignKeyInfo) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(localColumns, that.localColumns) &&
                Objects.equals(referencedTable, that.referencedTable) &&
                Objects.equals(referencedColumns, that.referencedColumns) &&
                Objects.equals(onDelete, that.onDelete) &&
                Objects.equals(onUpdate, that.onUpdate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, localColumns, referencedTable, referencedColumns, onDelete, onUpdate);
    }

    @Override
    public String toString() {
        return "ForeignKeyInfo[name=" + name + "]";
    }
}
