package com.ddlcomparator.model;

import java.util.List;
import java.util.Objects;

public final class DiffResult<T> {
    private final List<T> added;
    private final List<T> removed;
    private final List<String> changed;

    public DiffResult(List<T> added, List<T> removed, List<String> changed) {
        this.added = added;
        this.removed = removed;
        this.changed = changed;
    }

    public List<T> added() { return added; }
    public List<T> removed() { return removed; }
    public List<String> changed() { return changed; }

    public boolean hasChanges() {
        return !added.isEmpty() || !removed.isEmpty() || !changed.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiffResult)) return false;
        DiffResult<?> that = (DiffResult<?>) o;
        return Objects.equals(added, that.added) &&
                Objects.equals(removed, that.removed) &&
                Objects.equals(changed, that.changed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(added, removed, changed);
    }
}
