package com.ddlcomparator.model;

import java.util.Objects;

public final class TableDiff {
    public enum DiffStatus { ADDED, REMOVED, MODIFIED, IDENTICAL }

    private final String tableName;
    private final DiffStatus status;
    private final DiffResult<ColumnInfo> columnDiff;
    private final DiffResult<IndexInfo> indexDiff;
    private final DiffResult<ForeignKeyInfo> foreignKeyDiff;

    public TableDiff(String tableName, DiffStatus status,
                     DiffResult<ColumnInfo> columnDiff,
                     DiffResult<IndexInfo> indexDiff,
                     DiffResult<ForeignKeyInfo> foreignKeyDiff) {
        this.tableName = tableName;
        this.status = status;
        this.columnDiff = columnDiff;
        this.indexDiff = indexDiff;
        this.foreignKeyDiff = foreignKeyDiff;
    }

    public String tableName() { return tableName; }
    public DiffStatus status() { return status; }
    public DiffResult<ColumnInfo> columnDiff() { return columnDiff; }
    public DiffResult<IndexInfo> indexDiff() { return indexDiff; }
    public DiffResult<ForeignKeyInfo> foreignKeyDiff() { return foreignKeyDiff; }

    public boolean hasChanges() {
        return status != DiffStatus.IDENTICAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableDiff)) return false;
        TableDiff that = (TableDiff) o;
        return Objects.equals(tableName, that.tableName) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, status);
    }
}
