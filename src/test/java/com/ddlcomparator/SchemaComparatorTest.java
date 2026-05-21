package com.ddlcomparator;

import com.ddlcomparator.comparator.SchemaComparator;
import com.ddlcomparator.extractor.DdlExtractor;
import com.ddlcomparator.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaComparatorTest {

    private final DdlExtractor    extractor  = new DdlExtractor();
    private final SchemaComparator comparator = new SchemaComparator();

    // -------------------------------------------------------------------------
    // Indexes
    // -------------------------------------------------------------------------

    @Test
    void detects_added_index() {
        String a = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT,\n" +
                "    INDEX idx_user (user_id)\n" +
                ");";
        String b = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT,\n" +
                "    product_id INT,\n" +
                "    INDEX idx_user (user_id),\n" +
                "    INDEX idx_product (product_id)\n" +
                ");";

        SchemaReport report = compare(a, b);
        TableDiff diff = tableDiff(report, "orders");

        assertThat(diff.indexDiff().added()).hasSize(1);
        assertThat(diff.indexDiff().added().get(0).columns()).containsExactly("product_id");
    }

    @Test
    void detects_removed_index() {
        String a = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT,\n" +
                "    INDEX idx_user (user_id)\n" +
                ");";
        String b = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT\n" +
                ");";

        SchemaReport report = compare(a, b);
        TableDiff diff = tableDiff(report, "orders");

        assertThat(diff.indexDiff().removed()).hasSize(1);
    }

    @Test
    void detects_index_type_change() {
        String a = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT,\n" +
                "    INDEX idx_user (user_id)\n" +
                ");";
        String b = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT,\n" +
                "    UNIQUE KEY idx_user (user_id)\n" +
                ");";

        SchemaReport report = compare(a, b);
        TableDiff diff = tableDiff(report, "orders");

        assertThat(diff.indexDiff().changed()).hasSize(1);
        assertThat(diff.indexDiff().changed().get(0)).contains("INDEX").contains("UNIQUE");
    }

    // -------------------------------------------------------------------------
    // Foreign Keys
    // -------------------------------------------------------------------------

    @Test
    void detects_added_foreign_key() {
        String a = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT\n" +
                ");";
        String b = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT,\n" +
                "    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id)\n" +
                ");";

        SchemaReport report = compare(a, b);
        TableDiff diff = tableDiff(report, "orders");

        assertThat(diff.foreignKeyDiff().added()).hasSize(1);
        assertThat(diff.foreignKeyDiff().added().get(0).referencedTable()).isEqualTo("users");
    }

    @Test
    void detects_foreign_key_on_delete_change() {
        String a = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT,\n" +
                "    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE\n" +
                ");";
        String b = "CREATE TABLE orders (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    user_id INT,\n" +
                "    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL\n" +
                ");";

        SchemaReport report = compare(a, b);
        TableDiff diff = tableDiff(report, "orders");

        assertThat(diff.foreignKeyDiff().changed()).hasSize(1);
        assertThat(diff.foreignKeyDiff().changed().get(0))
                .contains("ON DELETE").contains("CASCADE").contains("SET NULL");
    }

    // -------------------------------------------------------------------------
    // Tables
    // -------------------------------------------------------------------------

    @Test
    void detects_added_and_removed_table() {
        String a = "CREATE TABLE alpha (id INT PRIMARY KEY);";
        String b = "CREATE TABLE beta  (id INT PRIMARY KEY);";

        SchemaReport report = compare(a, b);

        assertThat(report.addedCount()).isEqualTo(1);
        assertThat(report.removedCount()).isEqualTo(1);
    }

    @Test
    void identical_schemas_produce_no_changes() {
        String ddl = "CREATE TABLE users (\n" +
                "    id   INT PRIMARY KEY,\n" +
                "    name VARCHAR(255) NOT NULL,\n" +
                "    INDEX idx_name (name)\n" +
                ");";

        SchemaReport report = compare(ddl, ddl);
        assertThat(report.modifiedCount()).isZero();
        assertThat(report.addedCount()).isZero();
        assertThat(report.removedCount()).isZero();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchemaReport compare(String ddlA, String ddlB) {
        List<TableInfo> tablesA = extractor.extractFromString(ddlA);
        List<TableInfo> tablesB = extractor.extractFromString(ddlB);
        return comparator.compare("ENV_A", tablesA, "ENV_B", tablesB);
    }

    private TableDiff tableDiff(SchemaReport report, String tableName) {
        return report.tableDiffs().stream()
                .filter(d -> d.tableName().equalsIgnoreCase(tableName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Table not found: " + tableName));
    }
}
