package com.ddlcomparator.comparator;

import com.ddlcomparator.model.DiffResult;
import com.ddlcomparator.model.IndexInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IndexComparator {

    public DiffResult<IndexInfo> compare(List<IndexInfo> before, List<IndexInfo> after) {
        Map<String, IndexInfo> mapBefore = indexByName(before);
        Map<String, IndexInfo> mapAfter  = indexByName(after);

        List<IndexInfo> added   = new ArrayList<>();
        List<IndexInfo> removed = new ArrayList<>();
        List<String>    changed = new ArrayList<>();

        mapAfter.forEach((name, idxAfter) -> {
            IndexInfo idxBefore = mapBefore.get(name);
            if (idxBefore == null) {
                added.add(idxAfter);
            } else {
                boolean typeChanged = !idxBefore.type().equalsIgnoreCase(idxAfter.type());
                boolean colsChanged = !normalizedCols(idxBefore).equals(normalizedCols(idxAfter));
                if (typeChanged && colsChanged) {
                    changed.add(String.format("Index [%s] type changed: %s -> %s, columns changed: %s -> %s",
                            name, idxBefore.type(), idxAfter.type(), idxBefore.columns(), idxAfter.columns()));
                } else if (typeChanged) {
                    changed.add(String.format("Index [%s] type changed: %s -> %s",
                            name, idxBefore.type(), idxAfter.type()));
                } else if (colsChanged) {
                    changed.add(String.format("Index [%s] columns changed: %s -> %s",
                            name, idxBefore.columns(), idxAfter.columns()));
                }
            }
        });

        mapBefore.forEach((name, idx) -> {
            if (!mapAfter.containsKey(name)) removed.add(idx);
        });

        return new DiffResult<>(added, removed, changed);
    }

    private Map<String, IndexInfo> indexByName(List<IndexInfo> indexes) {
        return indexes.stream().collect(Collectors.toMap(
                idx -> idx.name() != null ? idx.name().toLowerCase() : idx.structuralKey(),
                idx -> idx,
                (a, b) -> a
        ));
    }

    private List<String> normalizedCols(IndexInfo idx) {
        return idx.columns().stream().map(String::toLowerCase).sorted().collect(Collectors.toList());
    }
}
