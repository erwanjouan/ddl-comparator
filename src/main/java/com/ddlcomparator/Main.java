package com.ddlcomparator;

import com.ddlcomparator.comparator.SchemaComparator;
import com.ddlcomparator.extractor.DdlExtractor;
import com.ddlcomparator.extractor.Dialect;
import com.ddlcomparator.model.SchemaReport;
import com.ddlcomparator.model.TableInfo;
import com.ddlcomparator.report.ConsoleReporter;
import com.ddlcomparator.report.JsonReporter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "ddl-comparator",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Compare SQL DDL schemas (tables, columns, indexes, foreign keys) between two environments."
)
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the first DDL file (env A)")
    private Path ddlA;

    @Parameters(index = "1", description = "Path to the second DDL file (env B)")
    private Path ddlB;

    @Option(names = {"-a", "--env-a"}, description = "Label for environment A", defaultValue = "ENV_A")
    private String envA;

    @Option(names = {"-b", "--env-b"}, description = "Label for environment B", defaultValue = "ENV_B")
    private String envB;

    @Option(names = {"-o", "--output"}, description = "Write JSON report to this file")
    private Path jsonOutput;

    @Option(names = {"--dialect"}, description = "SQL dialect: MYSQL, ORACLE (default: ORACLE)", defaultValue = "ORACLE")
    private Dialect dialect;

    @Option(names = {"--no-color"}, description = "Disable ANSI colors in console output")
    private boolean noColor;

    @Option(names = {"--only-changes"}, description = "Only show tables with differences (default: true)", defaultValue = "true")
    private boolean onlyChanges;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        DdlExtractor extractor = new DdlExtractor(dialect);

        System.out.printf("Parsing [%s] from %s...%n", envA, ddlA);
        List<TableInfo> tablesA = extractor.extractFromFile(ddlA);

        System.out.printf("Parsing [%s] from %s...%n", envB, ddlB);
        List<TableInfo> tablesB = extractor.extractFromFile(ddlB);

        System.out.printf("Found %d tables in %s, %d tables in %s.%n%n",
                tablesA.size(), envA, tablesB.size(), envB);

        SchemaReport report = new SchemaComparator().compare(envA, tablesA, envB, tablesB);

        // Console output
        new ConsoleReporter(System.out, !noColor).print(report);

        // Optional JSON output
        if (jsonOutput != null) {
            new JsonReporter().writeJson(report, jsonOutput);
            System.out.printf("%nJSON report written to: %s%n", jsonOutput);
        }

        // Exit 1 if differences found, 0 if schemas are identical
        return report.modifiedCount() > 0
                || report.addedCount() > 0
                || report.removedCount() > 0 ? 1 : 0;
    }
}
