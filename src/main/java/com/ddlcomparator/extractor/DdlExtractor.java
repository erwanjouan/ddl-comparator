package com.ddlcomparator.extractor;

import com.ddlcomparator.model.ColumnInfo;
import com.ddlcomparator.model.ForeignKeyInfo;
import com.ddlcomparator.model.IndexInfo;
import com.ddlcomparator.model.TableInfo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DdlExtractor {

    private static final Logger log = LoggerFactory.getLogger(DdlExtractor.class);

    /** Matches only the CREATE statements we care about — TABLE and INDEX. */
    private static final Pattern CREATE_STRUCTURAL = Pattern.compile(
            "\\bCREATE\\s+(?:GLOBAL\\s+)?(?:TEMPORARY\\s+)?TABLE\\b"
          + "|\\bCREATE\\s+(?:UNIQUE\\s+|BITMAP\\s+)?INDEX\\b",
            Pattern.CASE_INSENSITIVE);

    private final Dialect dialect;

    public DdlExtractor() {
        this(Dialect.ORACLE);
    }

    public DdlExtractor(Dialect dialect) {
        this.dialect = dialect;
    }

    public List<TableInfo> extractFromFile(Path path) throws IOException {
        String ddl = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return extractFromString(ddl);
    }

    public List<TableInfo> extractFromString(String ddl) {
        Map<String, List<IndexInfo>> externalIndexes = new LinkedHashMap<>();
        List<TableInfo> tables = new ArrayList<>();
        try {
            Statements stmts = CCJSqlParserUtil.parseStatements(sanitize(ddl));
            for (Statement stmt : stmts.getStatements()) {
                if (stmt instanceof CreateTable) {
                    CreateTable ct = (CreateTable) stmt;
                    tables.add(toTableInfo(ct));
                } else if (stmt instanceof CreateIndex) {
                    CreateIndex ci = (CreateIndex) stmt;
                    collectExternalIndex(ci, externalIndexes);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse DDL: {}", e.getMessage());
            throw new DdlParseException("DDL parsing failed", e);
        }
        return mergeExternalIndexes(tables, externalIndexes);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void collectExternalIndex(CreateIndex ci, Map<String, List<IndexInfo>> target) {
        if (ci.getTable() == null || ci.getIndex() == null) return;
        String tableName = ci.getTable().getName().toLowerCase();

        List<String> cols = ci.getIndex().getColumnsNames() != null
                ? ci.getIndex().getColumnsNames().stream().map(String::toLowerCase).collect(Collectors.toList())
                : Collections.<String>emptyList();

        String type = ci.getIndex().getType() != null ? ci.getIndex().getType() : "INDEX";
        target.computeIfAbsent(tableName, k -> new ArrayList<>())
              .add(new IndexInfo(ci.getIndex().getName(), type, cols));
    }

    private List<TableInfo> mergeExternalIndexes(List<TableInfo> tables,
                                                  Map<String, List<IndexInfo>> externalIndexes) {
        if (externalIndexes.isEmpty()) return tables;
        List<TableInfo> result = new ArrayList<>();
        for (TableInfo table : tables) {
            List<IndexInfo> extra = externalIndexes.get(table.nameLower());
            if (extra != null && !extra.isEmpty()) {
                List<IndexInfo> merged = new ArrayList<>(table.indexes());
                merged.addAll(extra);
                result.add(new TableInfo(table.name(), table.columns(), merged, table.foreignKeys()));
            } else {
                result.add(table);
            }
        }
        return result;
    }

    private TableInfo toTableInfo(CreateTable ct) {
        String tableName = ct.getTable().getName();
        List<ColumnInfo> columns  = extractColumns(ct);
        List<IndexInfo> indexes   = extractIndexes(ct);
        List<ForeignKeyInfo> fks  = extractForeignKeys(ct);
        return new TableInfo(tableName, columns, indexes, fks);
    }

    private List<ColumnInfo> extractColumns(CreateTable ct) {
        if (ct.getColumnDefinitions() == null) return Collections.emptyList();

        List<ColumnInfo> result = new ArrayList<>();
        for (ColumnDefinition col : ct.getColumnDefinitions()) {
            ColDataType type = col.getColDataType();
            List<String> specs = col.getColumnSpecs() != null
                    ? col.getColumnSpecs().stream().map(String::toUpperCase).collect(Collectors.toList())
                    : Collections.<String>emptyList();

            boolean nullable = !specs.contains("NOT") || !specs.contains("NULL");
            String defaultVal = extractDefault(specs);

            result.add(new ColumnInfo(
                    col.getColumnName(),
                    type.toString(),
                    nullable,
                    defaultVal,
                    specs
            ));
        }
        return result;
    }

    private List<IndexInfo> extractIndexes(CreateTable ct) {
        if (ct.getIndexes() == null) return Collections.emptyList();

        List<IndexInfo> result = new ArrayList<>();
        for (Index idx : ct.getIndexes()) {
            if (idx instanceof ForeignKeyIndex) continue;

            List<String> cols = idx.getColumnsNames() != null
                    ? idx.getColumnsNames().stream().map(String::toLowerCase).collect(Collectors.toList())
                    : Collections.<String>emptyList();

            result.add(new IndexInfo(idx.getName(), idx.getType(), cols));
        }
        return result;
    }

    private List<ForeignKeyInfo> extractForeignKeys(CreateTable ct) {
        if (ct.getIndexes() == null) return Collections.emptyList();

        List<ForeignKeyInfo> result = new ArrayList<>();
        for (Index idx : ct.getIndexes()) {
            if (!(idx instanceof ForeignKeyIndex)) continue;
            ForeignKeyIndex fk = (ForeignKeyIndex) idx;

            List<String> localCols = fk.getColumnsNames() != null
                    ? fk.getColumnsNames().stream().map(String::toLowerCase).collect(Collectors.toList())
                    : Collections.<String>emptyList();

            String refTable = fk.getTable() != null
                    ? fk.getTable().getName().toLowerCase() : "";

            List<String> refCols = fk.getReferencedColumnNames() != null
                    ? fk.getReferencedColumnNames().stream().map(String::toLowerCase).collect(Collectors.toList())
                    : Collections.<String>emptyList();

            String onDelete = fk.getOnDeleteReferenceOption() != null
                    ? fk.getOnDeleteReferenceOption().toString().toUpperCase() : null;
            String onUpdate = fk.getOnUpdateReferenceOption() != null
                    ? fk.getOnUpdateReferenceOption().toString().toUpperCase() : null;

            result.add(new ForeignKeyInfo(fk.getName(), localCols, refTable, refCols, onDelete, onUpdate));
        }
        return result;
    }

    private String extractDefault(List<String> specs) {
        for (int i = 0; i < specs.size() - 1; i++) {
            if ("DEFAULT".equalsIgnoreCase(specs.get(i))) {
                return specs.get(i + 1);
            }
        }
        return null;
    }

    private String sanitize(String ddl) {
        switch (dialect) {
            case ORACLE: return sanitizeOracle(ddl);
            default:     return sanitizeMysql(ddl);
        }
    }

    private String sanitizeMysql(String ddl) {
        return ddl
                .replaceAll("(?i)ENGINE\\s*=\\s*\\w+", "")
                .replaceAll("(?i)DEFAULT\\s+CHARSET\\s*=\\s*\\w+", "")
                .replaceAll("(?i)COLLATE\\s*=\\s*\\S+", "")
                .replaceAll("(?i)ROW_FORMAT\\s*=\\s*\\w+", "")
                .replaceAll("(?i)AUTO_INCREMENT\\s*=\\s*\\d+", "");
    }

    private String sanitizeOracle(String ddl) {
        String r = ddl;
        // GLOBAL TEMPORARY appears before the column list — normalise to plain TABLE
        r = r.replaceAll("(?i)\\bGLOBAL\\s+TEMPORARY\\b", "");
        // Inline column-level extensions that live inside the CREATE TABLE (...) block
        r = r.replaceAll(
            "(?i)\\bGENERATED\\s+(?:ALWAYS|BY\\s+DEFAULT(?:\\s+ON\\s+NULL)?)\\s+AS\\s+IDENTITY(?:\\s*\\([^)]*\\))?",
            "");
        r = r.replaceAll("(?is)\\bXMLTYPE\\s+STORE\\s+AS[^,)]*(?:\\([^)]*\\))?", "XMLTYPE");
        r = r.replaceAll("(?i)\\bUSING\\s+INDEX(?:\\s+TABLESPACE\\s+(?:\"[^\"]+\"|\\w+))?", "");
        r = r.replaceAll("(?i)\\b(?:NOT\\s+)?DEFERRABLE\\b", "");
        r = r.replaceAll("(?i)\\bINITIALLY\\s+(?:DEFERRED|IMMEDIATE)\\b", "");
        r = r.replaceAll("(?i)\\bINVISIBLE\\b", "");
        // Everything between the column-list closing ')' and ';' is storage noise — drop it
        r = stripPostColumnListStorage(r);
        return r;
    }

    /**
     * Scans the DDL for CREATE TABLE and CREATE INDEX statements only.
     * For each one, keeps everything up to the closing ')' of the column/column-list
     * block and discards the rest (storage options, partitioning, etc.) up to the ';'.
     * All other statement types (CREATE TRIGGER, CREATE VIEW, CREATE SEQUENCE, etc.)
     * are silently dropped — they are irrelevant for structural comparison.
     */
    private String stripPostColumnListStorage(String ddl) {
        StringBuilder out = new StringBuilder(ddl.length());
        Matcher m = CREATE_STRUCTURAL.matcher(ddl);
        int i = 0;

        while (m.find(i)) {
            int createPos = m.start();

            int openParen = ddl.indexOf('(', createPos);
            if (openParen < 0) break;

            int closeParen = findMatchingCloseParen(ddl, openParen);
            if (closeParen < 0) break;

            out.append(ddl, createPos, closeParen + 1);
            out.append(";\n");

            int semi = ddl.indexOf(';', closeParen + 1);
            i = (semi < 0) ? ddl.length() : semi + 1;
        }
        return out.toString();
    }

    /** Returns the index of the ')' that matches the '(' at {@code openIdx}, respecting nesting and quotes. */
    private int findMatchingCloseParen(String ddl, int openIdx) {
        int depth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = openIdx; i < ddl.length(); i++) {
            char c = ddl.charAt(i);
            if (inSingle) {
                if (c == '\'' && i + 1 < ddl.length() && ddl.charAt(i + 1) == '\'') {
                    i++; // escaped ''
                } else if (c == '\'') {
                    inSingle = false;
                }
            } else if (inDouble) {
                if (c == '"') inDouble = false;
            } else if (c == '\'') {
                inSingle = true;
            } else if (c == '"') {
                inDouble = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (--depth == 0) return i;
            }
        }
        return -1;
    }
}
