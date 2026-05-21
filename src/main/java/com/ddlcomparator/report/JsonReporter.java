package com.ddlcomparator.report;

import com.ddlcomparator.model.SchemaReport;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Path;

public class JsonReporter {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

    public String toJson(SchemaReport report) throws IOException {
        return mapper.writeValueAsString(report);
    }

    public void writeJson(SchemaReport report, Path output) throws IOException {
        mapper.writeValue(output.toFile(), report);
    }
}
