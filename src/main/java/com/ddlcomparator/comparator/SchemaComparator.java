package com.ddlcomparator.comparator;

import com.ddlcomparator.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SchemaComparator {

    private final ColumnComparator     columnComparator     = new ColumnComparator();
    private final IndexComparator      indexComparator      = new IndexComparator();
    private final ForeignKeyComparator foreignKeyComparator = new ForeignKeyComparator();

    public SchemaReport compare(String envA, List<TableInfo> tablesA,
                                String envB, List<TableInfo> tablesB) {

        Map<String, TableInfo> mapA = index(tablesA);
        Map<String, TableInfo> mapB = index(tablesB);

        List<TableDiff> diffs = new ArrayList<>();

        // Tables present in A — check modified or removed
        mapA.forEach((name, tableA) -> {
            TableInfo tableB = mapB.get(name);
            if (tableB == null) {
                diffs.add(removedTable(tableA));
            } else {
                diffs.add(diffTable(tableA, tableB));
            }
        });

        // Tables only in B — added
        mapB.forEach((name, tableB) -> {
            if (!mapA.containsKey(name)) {
                diffs.add(addedTable(tableB));
            }
        });

        diffs.sort((a, b) -> a.tableName().compareToIgnoreCase(b.tableName()));
        return new SchemaReport(envA, envB, diffs);
    }

    // -------------------------------------------------------------------------

    private TableDiff diffTable(TableInfo a, TableInfo b) {
        DiffResult<ColumnInfo>     colDiff = columnComparator.compare(a.columns(), b.columns());
        DiffResult<IndexInfo>      idxDiff = indexComparator.compare(a.indexes(), b.indexes());
        DiffResult<ForeignKeyInfo> fkDiff  = foreignKeyComparator.compare(a.foreignKeys(), b.foreignKeys());

        boolean hasChanges = colDiff.hasChanges() || idxDiff.hasChanges() || fkDiff.hasChanges();
        TableDiff.DiffStatus status = hasChanges ? TableDiff.DiffStatus.MODIFIED : TableDiff.DiffStatus.IDENTICAL;

        return new TableDiff(a.name(), status, colDiff, idxDiff, fkDiff);
    }

    private TableDiff addedTable(TableInfo t) {
        return new TableDiff(t.name(), TableDiff.DiffStatus.ADDED,
                empty(), empty(), emptyFk());
    }

    private TableDiff removedTable(TableInfo t) {
        return new TableDiff(t.name(), TableDiff.DiffStatus.REMOVED,
                empty(), empty(), emptyFk());
    }

    private Map<String, TableInfo> index(List<TableInfo> tables) {
        return tables.stream().collect(Collectors.toMap(
                TableInfo::nameLower,
                t -> t,
                (a, b) -> a
        ));
    }

    private <T> DiffResult<T> empty() {
        return new DiffResult<>(Collections.<T>emptyList(), Collections.<T>emptyList(), Collections.<String>emptyList());
    }

    private DiffResult<ForeignKeyInfo> emptyFk() {
        return new DiffResult<>(Collections.<ForeignKeyInfo>emptyList(), Collections.<ForeignKeyInfo>emptyList(), Collections.<String>emptyList());
    }
}
