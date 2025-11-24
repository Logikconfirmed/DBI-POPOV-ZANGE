package com.example.dbi.BüchereiVerwaltung.Service;

import com.example.dbi.BüchereiVerwaltung.dto.BenchmarkMetric;
import com.example.dbi.BüchereiVerwaltung.dto.BenchmarkSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.cloud.remote-url")
public class CloudBenchmarkClient {

    private static final Logger log = LoggerFactory.getLogger(CloudBenchmarkClient.class);

    private final RestTemplate restTemplate;
    private final String remoteUrl;

    public CloudBenchmarkClient(RestTemplateBuilder builder,
                                @Value("${app.cloud.remote-url}") String remoteUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.remoteUrl = remoteUrl.endsWith("/") ? remoteUrl.substring(0, remoteUrl.length() - 1) : remoteUrl;
    }

    public void compareWithRemote(BenchmarkSummary localSummary) {
        try {
            BenchmarkSummary remoteSummary = fetchRemoteSummary();
            if (remoteSummary == null) {
                log.warn("Konnte keine Cloud-Benchmarkdaten abrufen.");
                return;
            }
            log.info("Vergleich lokaler Benchmark mit Cloud-Instanz ({}).", remoteUrl);
            Map<String, BenchmarkMetric> remoteMetrics = remoteSummary.getMetrics().stream()
                    .collect(Collectors.toMap(this::metricKey, Function.identity(), (a, b) -> a));
            localSummary.getMetrics().forEach(localMetric -> {
                BenchmarkMetric remote = remoteMetrics.get(metricKey(localMetric));
                if (remote != null) {
                    long delta = localMetric.getDurationMs() - remote.getDurationMs();
                    log.info("Cloud Vergleich {}: lokal {}ms / cloud {}ms (Δ {}ms)",
                            localMetric.getOperation(),
                            localMetric.getDurationMs(),
                            remote.getDurationMs(),
                            delta);
                }
            });
        } catch (Exception ex) {
            log.warn("Remote Benchmark Vergleich fehlgeschlagen: {}", ex.getMessage());
        }
    }

    private BenchmarkSummary fetchRemoteSummary() {
        ResponseEntity<BenchmarkSummary> response =
                restTemplate.getForEntity(remoteUrl + "/api/benchmark/summary", BenchmarkSummary.class);
        return response.getBody();
    }

    private String metricKey(BenchmarkMetric metric) {
        return metric.getDatastore() + ":" + metric.getOperation();
    }
}

