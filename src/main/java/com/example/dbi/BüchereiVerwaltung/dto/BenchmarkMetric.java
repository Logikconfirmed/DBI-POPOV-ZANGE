package com.example.dbi.BüchereiVerwaltung.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BenchmarkMetric {
    private final String datastore;
    private final String operation;
    private final long durationMs;
    private final long affectedCount;

    @JsonCreator
    public BenchmarkMetric(@JsonProperty("datastore") String datastore,
                           @JsonProperty("operation") String operation,
                           @JsonProperty("durationMs") long durationMs,
                           @JsonProperty("affectedCount") long affectedCount) {
        this.datastore = datastore;
        this.operation = operation;
        this.durationMs = durationMs;
        this.affectedCount = affectedCount;
    }

    public String getDatastore() {
        return datastore;
    }

    public String getOperation() {
        return operation;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getAffectedCount() {
        return affectedCount;
    }
}

