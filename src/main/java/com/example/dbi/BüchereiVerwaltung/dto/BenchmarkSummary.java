package com.example.dbi.BüchereiVerwaltung.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class BenchmarkSummary {
    private final long executedAt;
    private final List<Integer> datasetSizes;
    private final List<BenchmarkMetric> metrics;

    @JsonCreator
    public BenchmarkSummary(@JsonProperty("executedAt") long executedAt,
                            @JsonProperty("datasetSizes") List<Integer> datasetSizes,
                            @JsonProperty("metrics") List<BenchmarkMetric> metrics) {
        this.executedAt = executedAt;
        this.datasetSizes = datasetSizes == null ? List.of() : List.copyOf(datasetSizes);
        this.metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }

    public static BenchmarkSummary empty() {
        return new BenchmarkSummary(Instant.now().toEpochMilli(), Collections.emptyList(), Collections.emptyList());
    }

    public long getExecutedAt() {
        return executedAt;
    }

    public List<Integer> getDatasetSizes() {
        return datasetSizes;
    }

    public List<BenchmarkMetric> getMetrics() {
        return metrics;
    }
}

