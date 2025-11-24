package com.example.dbi.BüchereiVerwaltung.web;

import com.example.dbi.BüchereiVerwaltung.Service.LibraryPerformanceTester;
import com.example.dbi.BüchereiVerwaltung.dto.BenchmarkSummary;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final LibraryPerformanceTester tester;

    public BenchmarkController(LibraryPerformanceTester tester) {
        this.tester = tester;
    }

    @PostMapping("/run")
    public BenchmarkSummary run(@RequestBody(required = false) BenchmarkRequest request) {
        if (request == null || request.sizes == null || request.sizes.length == 0) {
            return tester.runBenchmark();
        }
        return tester.runBenchmark(request.sizes);
    }

    @GetMapping("/summary")
    public BenchmarkSummary summary() {
        return tester.getLastSummary();
    }

    public static class BenchmarkRequest {
        private int[] sizes;

        public int[] getSizes() {
            return sizes;
        }

        public void setSizes(int[] sizes) {
            this.sizes = sizes;
        }
    }
}

