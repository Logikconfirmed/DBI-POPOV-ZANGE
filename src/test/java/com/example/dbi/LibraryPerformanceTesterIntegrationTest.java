package com.example.dbi;

import com.example.dbi.BüchereiVerwaltung.Service.LibraryPerformanceTester;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Executes the full CRUD benchmark once to document timings as geforderte "Tracking via Unit Test".
 * Uses small datasets to keep CI fast while still exercising jede Operation.
 */
@SpringBootTest
@ActiveProfiles("test")
class LibraryPerformanceTesterIntegrationTest {

    @Autowired
    private LibraryPerformanceTester tester;

    @Test
    void runsFullBenchmarkWithSmallDatasets() {
        tester.runBenchmark(50, 200);
    }
}

