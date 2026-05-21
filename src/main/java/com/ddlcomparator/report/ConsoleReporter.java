package com.ddlcomparator.report;

import com.ddlcomparator.model.*;

import java.io.PrintStream;

public class ConsoleReporter {

    private static final String RESET  = "[0m";
    private static final String RED    = "[31m";
    private static final String GREEN  = "[32m";
    private static final String YELLOW = "[33m";
    private static final String CYAN   = "[36m";
    private static final String BOLD   = "[1m";

    private final PrintStream out;
    private final boolean useColors;

    public ConsoleReporter(PrintStream out, boolean useColors) {
        this.out = out;
        this.useColors = useColors;
    }

    public ConsoleReporter() {
        this(System.out, true);
    }

    public void print(SchemaReport report) {
        out.println();
        out.printf("%s=== DDL Comparison: %s  vs  %s ===%s%n",
                bold(), report.envA(), report.envB(), reset());
        out.printf("Tables added: %s%d%s  |  removed: %s%d%s  |  modified: %s%d%s%n%n",
                green(), report.addedCount(), reset(),
                red(), report.removedCount(), reset(),
                yellow(), report.modifiedCount(), reset());

        for (TableDiff td : report.tableDiffs()) {
            if (!td.hasChanges()) continue;
            printTableDiff(td);
        }

        long identical = report.tableDiffs().stream()
                .filter(t -> t.status() == TableDiff.DiffStatus.IDENTICAL).count();
        if (identical > 0) {
            out.printf("%n%s(+%d identical tables not shown)%s%n", cyan(), identical, reset());
        }
    }

    private void printTableDiff(TableDiff td) {
        String statusBadge;
        switch (td.status()) {
            case ADDED:    statusBadge = green()  + "[+ADDED]"    + reset(); break;
            case REMOVED:  statusBadge = red()    + "[-REMOVED]"  + reset(); break;
            case MODIFIED: statusBadge = yellow() + "[~MODIFIED]" + reset(); break;
            default:       statusBadge = "";
        }
        out.printf("%s TABLE: %s %s%s%n", bold(), td.tableName().toUpperCase(), statusBadge, reset());
        out.println("  " + repeat('─', 60));

        if (td.status() == TableDiff.DiffStatus.ADDED ||
            td.status() == TableDiff.DiffStatus.REMOVED) {
            out.println();
            return;
        }

        printSection("COLUMNS",      td.columnDiff());
        printSection("INDEXES",      td.indexDiff());
        printSection("FOREIGN KEYS", td.foreignKeyDiff());
        out.println();
    }

    private <T> void printSection(String title, DiffResult<T> diff) {
        if (!diff.hasChanges()) return;

        out.printf("  %s%s%s:%n", cyan(), title, reset());
        for (T item : diff.added()) {
            out.printf("    %s+ %s%s%n", green(), format(item), reset());
        }
        for (T item : diff.removed()) {
            out.printf("    %s- %s%s%n", red(), format(item), reset());
        }
        for (String msg : diff.changed()) {
            out.printf("    %s~ %s%s%n", yellow(), msg, reset());
        }
    }

    private String format(Object item) {
        if (item instanceof ColumnInfo) {
            ColumnInfo c = (ColumnInfo) item;
            return String.format("Column [%s] %s", c.name(), c.dataType());
        } else if (item instanceof IndexInfo) {
            IndexInfo i = (IndexInfo) item;
            return String.format("Index [%s] type=%s cols=%s", i.name(), i.type(), i.columns());
        } else if (item instanceof ForeignKeyInfo) {
            ForeignKeyInfo f = (ForeignKeyInfo) item;
            return String.format("FK [%s] %s -> %s(%s) onDelete=%s onUpdate=%s",
                    f.name(), f.localColumns(), f.referencedTable(),
                    f.referencedColumns(), f.onDelete(), f.onUpdate());
        } else {
            return item.toString();
        }
    }

    private static String repeat(char ch, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(ch);
        return sb.toString();
    }

    private String green()  { return useColors ? GREEN  : ""; }
    private String red()    { return useColors ? RED    : ""; }
    private String yellow() { return useColors ? YELLOW : ""; }
    private String cyan()   { return useColors ? CYAN   : ""; }
    private String bold()   { return useColors ? BOLD   : ""; }
    private String reset()  { return useColors ? RESET  : ""; }
}
