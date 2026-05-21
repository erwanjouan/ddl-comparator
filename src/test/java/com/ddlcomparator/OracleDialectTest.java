package com.ddlcomparator;

import com.ddlcomparator.comparator.SchemaComparator;
import com.ddlcomparator.extractor.DdlExtractor;
import com.ddlcomparator.extractor.Dialect;
import com.ddlcomparator.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OracleDialectTest {

    private final DdlExtractor    extractor  = new DdlExtractor(Dialect.ORACLE);
    private final SchemaComparator comparator = new SchemaComparator();

    @Test
    void parses_oracle_column_types() {
        String ddl = "CREATE TABLE employees (\n" +
                "    employee_id NUMBER(6) NOT NULL,\n" +
                "    first_name VARCHAR2(20),\n" +
                "    hire_date DATE NOT NULL\n" +
                ");";

        List<TableInfo> tables = extractor.extractFromString(ddl);

        assertThat(tables).hasSize(1);
        TableInfo t = tables.get(0);
        assertThat(t.columns()).hasSize(3);
        assertThat(t.columns().get(0).name()).isEqualTo("employee_id");
        assertThat(t.columns().get(0).dataType()).containsIgnoringCase("NUMBER");
        assertThat(t.columns().get(1).dataType()).containsIgnoringCase("VARCHAR2");
    }

    @Test
    void strips_oracle_storage_options() {
        String ddl = "CREATE TABLE employees (\n" +
                "    employee_id NUMBER(6) NOT NULL,\n" +
                "    name VARCHAR2(100)\n" +
                ")\n" +
                "SEGMENT CREATION IMMEDIATE\n" +
                "PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255\n" +
                "NOCOMPRESS LOGGING\n" +
                "STORAGE (INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645)\n" +
                "TABLESPACE users;";

        List<TableInfo> tables = extractor.extractFromString(ddl);

        assertThat(tables).hasSize(1);
        assertThat(tables.get(0).columns()).hasSize(2);
    }

    @Test
    void parses_standalone_create_index() {
        String ddl = "CREATE TABLE employees (\n" +
                "    employee_id NUMBER(6) NOT NULL,\n" +
                "    last_name VARCHAR2(25) NOT NULL,\n" +
                "    first_name VARCHAR2(20)\n" +
                ");\n" +
                "CREATE INDEX emp_name_idx ON employees (last_name, first_name);";

        List<TableInfo> tables = extractor.extractFromString(ddl);

        assertThat(tables).hasSize(1);
        TableInfo t = tables.get(0);
        assertThat(t.indexes()).hasSize(1);
        assertThat(t.indexes().get(0).name()).isEqualTo("emp_name_idx");
        assertThat(t.indexes().get(0).columns()).containsExactly("last_name", "first_name");
    }

    @Test
    void parses_unique_create_index() {
        String ddl = "CREATE TABLE employees (\n" +
                "    employee_id NUMBER(6) NOT NULL,\n" +
                "    email VARCHAR2(50) NOT NULL\n" +
                ");\n" +
                "CREATE UNIQUE INDEX emp_email_uk ON employees (email);";

        List<TableInfo> tables = extractor.extractFromString(ddl);

        assertThat(tables).hasSize(1);
        TableInfo t = tables.get(0);
        assertThat(t.indexes()).hasSize(1);
        assertThat(t.indexes().get(0).type()).containsIgnoringCase("UNIQUE");
        assertThat(t.indexes().get(0).columns()).containsExactly("email");
    }

    @Test
    void parses_oracle_foreign_key() {
        String ddl = "CREATE TABLE orders (\n" +
                "    order_id NUMBER(10) NOT NULL,\n" +
                "    customer_id NUMBER(10),\n" +
                "    CONSTRAINT orders_pk PRIMARY KEY (order_id),\n" +
                "    CONSTRAINT ord_cust_fk FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE\n" +
                ");";

        List<TableInfo> tables = extractor.extractFromString(ddl);

        assertThat(tables).hasSize(1);
        TableInfo t = tables.get(0);
        assertThat(t.foreignKeys()).hasSize(1);
        assertThat(t.foreignKeys().get(0).referencedTable()).isEqualTo("customers");
        assertThat(t.foreignKeys().get(0).onDelete()).isEqualTo("CASCADE");
    }

    @Test
    void detects_added_index_in_oracle_schemas() {
        String a = "CREATE TABLE orders (\n" +
                "    order_id NUMBER(10) NOT NULL,\n" +
                "    user_id NUMBER(10),\n" +
                "    CONSTRAINT orders_pk PRIMARY KEY (order_id)\n" +
                ");\n" +
                "CREATE INDEX idx_user ON orders (user_id);";

        String b = "CREATE TABLE orders (\n" +
                "    order_id NUMBER(10) NOT NULL,\n" +
                "    user_id NUMBER(10),\n" +
                "    product_id NUMBER(10),\n" +
                "    CONSTRAINT orders_pk PRIMARY KEY (order_id)\n" +
                ");\n" +
                "CREATE INDEX idx_user ON orders (user_id);\n" +
                "CREATE INDEX idx_product ON orders (product_id);";

        SchemaReport report = compare(a, b);
        TableDiff diff = tableDiff(report, "orders");

        assertThat(diff.columnDiff().added()).hasSize(1);
        assertThat(diff.indexDiff().added()).hasSize(1);
        assertThat(diff.indexDiff().added().get(0).columns()).containsExactly("product_id");
    }

    @Test
    void identical_oracle_schemas_produce_no_changes() {
        String ddl = "CREATE TABLE products (\n" +
                "    product_id NUMBER(10) NOT NULL,\n" +
                "    name VARCHAR2(100) NOT NULL,\n" +
                "    price NUMBER(10,2),\n" +
                "    CONSTRAINT products_pk PRIMARY KEY (product_id)\n" +
                ")\n" +
                "TABLESPACE users PCTFREE 10 NOCOMPRESS LOGGING;\n" +
                "CREATE INDEX idx_name ON products (name);";

        SchemaReport report = compare(ddl, ddl);

        assertThat(report.modifiedCount()).isZero();
        assertThat(report.addedCount()).isZero();
        assertThat(report.removedCount()).isZero();
    }

    // -------------------------------------------------------------------------

    private SchemaReport compare(String ddlA, String ddlB) {
        List<TableInfo> tablesA = extractor.extractFromString(ddlA);
        List<TableInfo> tablesB = extractor.extractFromString(ddlB);
        return comparator.compare("DB_A", tablesA, "DB_B", tablesB);
    }

    private TableDiff tableDiff(SchemaReport report, String tableName) {
        return report.tableDiffs().stream()
                .filter(d -> d.tableName().equalsIgnoreCase(tableName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Table not found: " + tableName));
    }
}
