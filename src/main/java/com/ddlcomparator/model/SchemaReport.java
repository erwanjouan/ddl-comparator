package com.ddlcomparator.model;

import java.util.List;
import java.util.Objects;

public final class SchemaReport {
    private final String envA;
    private final String envB;
    private final List<TableDiff> tableDiffs;

    public SchemaReport(String envA, String envB, List<TableDiff> tableDiffs) {
        this.envA = envA;
        this.envB = envB;
        this.tableDiffs = tableDiffs;
    }

    public String envA() { return envA; }
    public String envB() { return envB; }
    public List<TableDiff> tableDiffs() { return tableDiffs; }

    public long modifiedCount() {
        return tableDiffs.stream().filter(TableDiff::hasChanges).count();
    }

    public long addedCount() {
        return tableDiffs.stream()
                .filter(d -> d.status() == TableDiff.DiffStatus.ADDED).count();
    }

    public long removedCount() {
        return tableDiffs.stream()
                .filter(d -> d.status() == TableDiff.DiffStatus.REMOVED).count();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemaReport)) return false;
        SchemaReport that = (SchemaReport) o;
        return Objects.equals(envA, that.envA) &&
                Objects.equals(envB, that.envB) &&
                Objects.equals(tableDiffs, that.tableDiffs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(envA, envB, tableDiffs);
    }
}
