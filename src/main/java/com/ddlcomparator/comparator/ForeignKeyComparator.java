package com.ddlcomparator.comparator;

import com.ddlcomparator.model.DiffResult;
import com.ddlcomparator.model.ForeignKeyInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ForeignKeyComparator {

    public DiffResult<ForeignKeyInfo> compare(List<ForeignKeyInfo> before, List<ForeignKeyInfo> after) {
        Map<String, ForeignKeyInfo> mapBefore = index(before);
        Map<String, ForeignKeyInfo> mapAfter  = index(after);

        List<ForeignKeyInfo> added   = new ArrayList<>();
        List<ForeignKeyInfo> removed = new ArrayList<>();
        List<String>         changed = new ArrayList<>();

        mapAfter.forEach((key, fk) -> {
            if (!mapBefore.containsKey(key)) added.add(fk);
        });

        mapBefore.forEach((key, fk) -> {
            if (!mapAfter.containsKey(key)) {
                removed.add(fk);
            } else {
                detectChanges(fk, mapAfter.get(key), changed);
            }
        });

        return new DiffResult<>(added, removed, changed);
    }

    private void detectChanges(ForeignKeyInfo before, ForeignKeyInfo after, List<String> changes) {
        String label = String.format("[%s → %s(%s)]",
                before.localColumns(), before.referencedTable(), before.referencedColumns());

        if (!Objects.equals(before.onDelete(), after.onDelete())) {
            changes.add(String.format("FK %s ON DELETE changed: %s -> %s",
                    label, before.onDelete(), after.onDelete()));
        }
        if (!Objects.equals(before.onUpdate(), after.onUpdate())) {
            changes.add(String.format("FK %s ON UPDATE changed: %s -> %s",
                    label, before.onUpdate(), after.onUpdate()));
        }
    }

    /** Key = structural key: local cols → refTable(refCols), ignores name and referential actions */
    private Map<String, ForeignKeyInfo> index(List<ForeignKeyInfo> fks) {
        return fks.stream().collect(Collectors.toMap(
                ForeignKeyInfo::structuralKey,
                fk -> fk,
                (a, b) -> a
        ));
    }
}
