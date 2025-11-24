package com.example.dbi.BüchereiVerwaltung.Service;

import com.example.dbi.BüchereiVerwaltung.dto.TitleYearProjection;
import com.example.dbi.BüchereiVerwaltung.model.Book;
import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import com.example.dbi.BüchereiVerwaltung.repositories.BookRepositoryJpa;
import com.example.dbi.BüchereiVerwaltung.repositoriesMongo.BookRepoMongo;
import com.example.dbi.BüchereiVerwaltung.seed.Seedgenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryPerformanceTester {

    private static final String FILTER_TERM = "1";
    private static final int DELETE_BATCH = 5;

    private final Seedgenerator seedgenerator;
    private final BookRepositoryJpa bookRepo;
    private final BookRepoMongo bookRepoMongo;

    public LibraryPerformanceTester(Seedgenerator seedgenerator,
                                    BookRepositoryJpa bookRepo,
                                    BookRepoMongo bookRepoMongo) {
        this.seedgenerator = seedgenerator;
        this.bookRepo = bookRepo;
        this.bookRepoMongo = bookRepoMongo;
    }

    public void runBenchmark(int... sizes) {
        int[] normalizedSizes = normalizeSizes(sizes);
        for (int size : normalizedSizes) {
            System.out.println("==== RUN FOR SIZE: " + size + " ====");

            OperationMetric relWrite = runWriteTestRelational(size);
            OperationMetric mongoWrite = runWriteTestMongo(size);
            reportComparison("WRITE-" + size, relWrite, mongoWrite);

            List<OperationMetric> relFinds = runFindsRelational();
            List<OperationMetric> mongoFinds = runFindsMongo();
            compareMetricLists(relFinds, mongoFinds);

            OperationMetric relUpdate = runUpdateRelational();
            OperationMetric mongoUpdate = runUpdateMongo();
            reportComparison("UPDATE", relUpdate, mongoUpdate);

            OperationMetric relDelete = runDeleteRelational();
            OperationMetric mongoDelete = runDeleteMongo();
            reportComparison("DELETE", relDelete, mongoDelete);

            System.out.println();
        }
        System.out.println("=== BENCHMARK DONE ===");
    }

    private OperationMetric runWriteTestRelational(int bookCount) {
        long duration = timeNs(() -> seedgenerator.seedRelational(bookCount));
        return logMetric("Relational", "WRITE_" + bookCount, duration, bookCount);
    }

    private List<OperationMetric> runFindsRelational() {
        List<OperationMetric> metrics = new ArrayList<>();

        long start = System.nanoTime();
        List<Book> allBooks = bookRepo.findAll();
        metrics.add(logMetric("Relational", "FIND_ALL", System.nanoTime() - start, allBooks.size()));

        start = System.nanoTime();
        List<Book> filteredBooks = bookRepo.findByTitleContainingIgnoreCase(FILTER_TERM);
        metrics.add(logMetric("Relational", "FIND_FILTER", System.nanoTime() - start, filteredBooks.size()));

        start = System.nanoTime();
        List<TitleYearProjection> projection = mapBooksToProjection(
                bookRepo.findByTitleContainingIgnoreCase(FILTER_TERM));
        metrics.add(logMetric("Relational", "FIND_FILTER_PROJECTION", System.nanoTime() - start, projection.size()));

        start = System.nanoTime();
        List<TitleYearProjection> projectionSorted = sortProjectionDesc(
                mapBooksToProjection(bookRepo.findByTitleContainingIgnoreCase(FILTER_TERM)));
        metrics.add(logMetric("Relational", "FIND_FILTER_PROJECTION_SORT", System.nanoTime() - start, projectionSorted.size()));

        return metrics;
    }

    private OperationMetric runUpdateRelational() {
        List<Book> books = bookRepo.findAll();
        if (books.isEmpty()) {
            System.out.println("Relational Update übersprungen – keine Daten vorhanden");
            return new OperationMetric("UPDATE", 0, 0);
        }

        long duration = timeNs(() -> {
            books.forEach(book -> book.setReleaseYear(book.getReleaseYear() + 1));
            bookRepo.saveAll(books);
        });

        int newestYear = books.stream().map(Book::getReleaseYear)
                .max(Comparator.naturalOrder()).orElse(0);
        System.out.println("Relational Update max releaseYear => " + newestYear);
        return logMetric("Relational", "UPDATE", duration, books.size());
    }

    @SuppressWarnings("null")
    private OperationMetric runDeleteRelational() {
        List<Book> books = bookRepo.findAll();
        if (books.isEmpty()) {
            System.out.println("Relational Delete übersprungen – keine Daten vorhanden");
            return new OperationMetric("DELETE", 0, 0);
        }

        List<Book> toDelete = books.stream()
                .limit(Math.min(DELETE_BATCH, books.size()))
                .collect(Collectors.toList());
        long duration = timeNs(() -> bookRepo.deleteAll(toDelete));
        return logMetric("Relational", "DELETE", duration, toDelete.size());
    }

    private OperationMetric runWriteTestMongo(int bookCount) {
        long duration = timeNs(() -> seedgenerator.seedMongo(bookCount));
        return logMetric("Mongo", "WRITE_" + bookCount, duration, bookCount);
    }

    private List<OperationMetric> runFindsMongo() {
        List<OperationMetric> metrics = new ArrayList<>();

        long start = System.nanoTime();
        List<BookDocument> allDocs = bookRepoMongo.findAll();
        metrics.add(logMetric("Mongo", "FIND_ALL", System.nanoTime() - start, allDocs.size()));

        start = System.nanoTime();
        List<BookDocument> filteredDocs = bookRepoMongo.findByTitleContainingIgnoreCase(FILTER_TERM);
        metrics.add(logMetric("Mongo", "FIND_FILTER", System.nanoTime() - start, filteredDocs.size()));

        start = System.nanoTime();
        List<TitleYearProjection> projection = mapDocumentsToProjection(
                bookRepoMongo.findByTitleContainingIgnoreCase(FILTER_TERM));
        metrics.add(logMetric("Mongo", "FIND_FILTER_PROJECTION", System.nanoTime() - start, projection.size()));

        start = System.nanoTime();
        List<TitleYearProjection> projectionSorted = sortProjectionDesc(
                mapDocumentsToProjection(bookRepoMongo.findByTitleContainingIgnoreCase(FILTER_TERM)));
        metrics.add(logMetric("Mongo", "FIND_FILTER_PROJECTION_SORT", System.nanoTime() - start, projectionSorted.size()));

        return metrics;
    }

    private OperationMetric runUpdateMongo() {
        List<BookDocument> documents = bookRepoMongo.findAll();
        if (documents.isEmpty()) {
            System.out.println("Mongo Update übersprungen – keine Daten vorhanden");
            return new OperationMetric("UPDATE", 0, 0);
        }

        long duration = timeNs(() -> {
            documents.forEach(doc -> {
                doc.setIsbn(doc.getIsbn() + "-U");
                doc.setReleaseYear(doc.getReleaseYear() + 1);
            });
            bookRepoMongo.saveAll(documents);
        });
        return logMetric("Mongo", "UPDATE", duration, documents.size());
    }

    @SuppressWarnings("null")
    private OperationMetric runDeleteMongo() {
        List<BookDocument> documents = bookRepoMongo.findAll();
        if (documents.isEmpty()) {
            System.out.println("Mongo Delete übersprungen – keine Daten vorhanden");
            return new OperationMetric("DELETE", 0, 0);
        }

        List<BookDocument> toDelete = documents.stream()
                .limit(Math.min(DELETE_BATCH, documents.size()))
                .collect(Collectors.toList());
        long duration = timeNs(() -> bookRepoMongo.deleteAll(toDelete));
        return logMetric("Mongo", "DELETE", duration, toDelete.size());
    }

    private void compareMetricLists(List<OperationMetric> relational, List<OperationMetric> mongo) {
        int limit = Math.min(relational.size(), mongo.size());
        for (int i = 0; i < limit; i++) {
            OperationMetric relMetric = relational.get(i);
            OperationMetric mongoMetric = mongo.get(i);
            reportComparison(relMetric.operation(), relMetric, mongoMetric);
        }
    }

    private void reportComparison(String label, OperationMetric relational, OperationMetric mongo) {
        long delta = relational.durationMs() - mongo.durationMs();
        System.out.println(">> Vergleich " + label + ": Relational "
                + relational.durationMs() + " ms / Mongo "
                + mongo.durationMs() + " ms (Δ " + delta + " ms)");
    }

    private OperationMetric logMetric(String datastore, String operation, long durationNanos, long affectedCount) {
        long durationMs = toMs(durationNanos);
        System.out.println(datastore + " " + operation + ": " + durationMs + " ms (" + affectedCount + " Einträge)");
        return new OperationMetric(operation, durationMs, affectedCount);
    }

    private long timeNs(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return System.nanoTime() - start;
    }

    private long toMs(long nanos) {
        return nanos / 1_000_000;
    }

    private int[] normalizeSizes(int... sizes) {
        if (sizes == null || sizes.length == 0) {
            return new int[]{100, 1000};
        }
        return Arrays.stream(sizes)
                .filter(size -> size > 0)
                .toArray();
    }

    private List<TitleYearProjection> mapBooksToProjection(List<Book> source) {
        return source.stream()
                .map(book -> new TitleYearProjection(book.getTitle(), book.getReleaseYear()))
                .collect(Collectors.toList());
    }

    private List<TitleYearProjection> mapDocumentsToProjection(List<BookDocument> source) {
        return source.stream()
                .map(doc -> new TitleYearProjection(doc.getTitle(), doc.getReleaseYear()))
                .collect(Collectors.toList());
    }

    private List<TitleYearProjection> sortProjectionDesc(List<TitleYearProjection> projection) {
        return projection.stream()
                .sorted(Comparator.comparingInt(TitleYearProjection::releaseYear).reversed())
                .collect(Collectors.toList());
    }

    private static class OperationMetric {
        private final String operation;
        private final long durationMs;
        private final long affectedCount;

        private OperationMetric(String operation, long durationMs, long affectedCount) {
            this.operation = operation;
            this.durationMs = durationMs;
            this.affectedCount = affectedCount;
        }

        public String operation() {
            return operation;
        }

        public long durationMs() {
            return durationMs;
        }

        @SuppressWarnings("unused")
        public long affectedCount() {
            return affectedCount;
        }
    }
}
