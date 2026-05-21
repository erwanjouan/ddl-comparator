# DDL Comparator

A Java 8 + Maven tool to compare SQL DDL schemas (tables, columns, indexes, foreign keys) between two environments.

## Requirements

- Java 8+
- Maven 3.8+

## Build

```bash
mvn clean package -q
```

This produces a fat JAR at `target/ddl-comparator-1.0.0-SNAPSHOT.jar`.

## Run

```bash
# Basic comparison
java -jar target/ddl-comparator-1.0.0-SNAPSHOT.jar \
  src/main/resources/env_a.sql \
  src/main/resources/env_b.sql \
  --env-a PROD --env-b STAGING

# With JSON report output
java -jar target/ddl-comparator-1.0.0-SNAPSHOT.jar \
  src/main/resources/env_a.sql \
  src/main/resources/env_b.sql \
  --env-a PROD --env-b STAGING \
  --output report.json

# Without colors (useful for CI logs)
java -jar target/ddl-comparator-1.0.0-SNAPSHOT.jar \
  env_a.sql env_b.sql --no-color

# MySQL dialect (default is Oracle)
java -jar target/ddl-comparator-1.0.0-SNAPSHOT.jar \
  env_a.sql env_b.sql --dialect MYSQL
```

## Exit codes

| Code | Meaning                  |
|------|--------------------------|
| `0`  | Schemas are identical    |
| `1`  | Differences were found   |

Useful for CI pipelines — the build fails if schemas diverge.

## What it compares

| Element       | Detects                                                   |
|---------------|-----------------------------------------------------------|
| **Tables**    | Added / removed tables                                    |
| **Columns**   | Added / removed columns, type changes, nullability, defaults |
| **Indexes**   | Added / removed indexes, type changes (INDEX → UNIQUE)    |
| **Foreign Keys** | Added / removed FKs, ON DELETE / ON UPDATE changes    |

## Project structure

```
src/main/java/com/ddlcomparator/
├── Main.java                     ← CLI entry point (picocli)
├── model/
│   ├── ColumnInfo.java
│   ├── IndexInfo.java
│   ├── ForeignKeyInfo.java
│   ├── TableInfo.java
│   ├── DiffResult.java
│   ├── TableDiff.java
│   └── SchemaReport.java
├── extractor/
│   ├── DdlExtractor.java         ← JSQLParser-based DDL parser
│   └── Dialect.java              ← ORACLE (default) / MYSQL
├── comparator/
│   ├── ColumnComparator.java
│   ├── IndexComparator.java
│   ├── ForeignKeyComparator.java
│   └── SchemaComparator.java     ← Orchestrates the full diff
└── report/
    ├── ConsoleReporter.java      ← ANSI-colored terminal output
    └── JsonReporter.java         ← JSON output via Jackson
```

## Run tests

```bash
mvn test
```
# ddl-comparator
