package com.example.dbi;

import com.example.dbi.BüchereiVerwaltung.Service.LibraryPerformanceTester;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DbiApplication {
    public static void main(String[] args) {
        SpringApplication.run(DbiApplication.class, args);
    }

    @Bean
    CommandLineRunner run(LibraryPerformanceTester tester,
                          @Value("${app.runner.enabled:true}") boolean runnerEnabled) {
        return args -> {
            if (!runnerEnabled) {
                System.out.println("Benchmark runner deaktiviert (app.runner.enabled=false)");
                return;
            }
            tester.runBenchmark(100, 1000 /* , 100000 */);
        };
    }
}

