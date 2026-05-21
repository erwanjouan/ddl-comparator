package com.ddlcomparator.comparator;

import com.ddlcomparator.model.ColumnInfo;
import com.ddlcomparator.model.DiffResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ColumnComparator {

    public DiffResult<ColumnInfo> compare(List<ColumnInfo> before, List<ColumnInfo> after) {
        Map<String, ColumnInfo> mapBefore = index(before);
        Map<String, ColumnInfo> mapAfter  = index(after);

        List<ColumnInfo> added   = new ArrayList<>();
        List<ColumnInfo> removed = new ArrayList<>();
        List<String>     changed = new ArrayList<>();

        mapAfter.forEach((name, col) -> {
            if (!mapBefore.containsKey(name)) added.add(col);
        });

        mapBefore.forEach((name, col) -> {
            if (!mapAfter.containsKey(name)) {
                removed.add(col);
            } else {
                detectChanges(col, mapAfter.get(name), changed);
            }
        });

        return new DiffResult<>(added, removed, changed);
    }

    private void detectChanges(ColumnInfo before, ColumnInfo after, List<String> changes) {
        String name = before.name();

        if (!before.dataType().equalsIgnoreCase(after.dataType())) {
            changes.add(String.format("Column [%s] type changed: %s -> %s",
                    name, before.dataType(), after.dataType()));
        }
        if (before.nullable() != after.nullable()) {
            changes.add(String.format("Column [%s] nullability changed: %s -> %s",
                    name, nullable(before), nullable(after)));
        }
        if (!Objects.equals(before.defaultValue(), after.defaultValue())) {
            changes.add(String.format("Column [%s] default changed: %s -> %s",
                    name, before.defaultValue(), after.defaultValue()));
        }
    }

    private Map<String, ColumnInfo> index(List<ColumnInfo> cols) {
        return cols.stream().collect(Collectors.toMap(
                c -> c.name().toLowerCase(),
                c -> c,
                (a, b) -> a
        ));
    }

    private String nullable(ColumnInfo col) {
        return col.nullable() ? "NULLABLE" : "NOT NULL";
    }
}
