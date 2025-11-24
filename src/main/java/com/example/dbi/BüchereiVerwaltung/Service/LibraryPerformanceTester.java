package com.example.dbi.BüchereiVerwaltung.Service;

import com.example.dbi.BüchereiVerwaltung.dto.BenchmarkMetric;
import com.example.dbi.BüchereiVerwaltung.dto.BenchmarkSummary;
import com.example.dbi.BüchereiVerwaltung.dto.PublisherAggregationResult;
import com.example.dbi.BüchereiVerwaltung.dto.TitleYearProjection;
import com.example.dbi.BüchereiVerwaltung.model.Book;
import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import com.example.dbi.BüchereiVerwaltung.modelMongo.referencing.BookDocumentRef;
import com.example.dbi.BüchereiVerwaltung.repositories.BookRepositoryJpa;
import com.example.dbi.BüchereiVerwaltung.repositoriesMongo.BookRepoMongo;
import com.example.dbi.BüchereiVerwaltung.repositoriesMongo.BookRepoMongoRef;
import com.example.dbi.BüchereiVerwaltung.seed.Seedgenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class LibraryPerformanceTester {

    private static final String FILTER_TERM = "1";
    private static final int DELETE_BATCH = 5;

    private final Seedgenerator seedgenerator;
    private final BookRepositoryJpa bookRepo;
    private final BookRepoMongo bookRepoMongo;
    private final BookRepoMongoRef bookRepoMongoRef;
    private final MongoIndexManager mongoIndexManager;
    private final CloudBenchmarkClient cloudBenchmarkClient;

    private volatile BenchmarkSummary lastSummary = BenchmarkSummary.empty();
    private List<OperationMetric> currentRunMetrics = new ArrayList<>();

    public LibraryPerformanceTester(Seedgenerator seedgenerator,
                                    BookRepositoryJpa bookRepo,
                                    BookRepoMongo bookRepoMongo,
                                    BookRepoMongoRef bookRepoMongoRef,
                                    MongoIndexManager mongoIndexManager,
                                    List<CloudBenchmarkClient> cloudBenchmarkClient) {
        this.seedgenerator = seedgenerator;
        this.bookRepo = bookRepo;
        this.bookRepoMongo = bookRepoMongo;
        this.bookRepoMongoRef = bookRepoMongoRef;
        this.mongoIndexManager = mongoIndexManager;
        this.cloudBenchmarkClient = cloudBenchmarkClient.isEmpty() ? null : cloudBenchmarkClient.get(0);
    }

    public BenchmarkSummary runBenchmark(int... sizes) {
        int[] normalizedSizes = normalizeSizes(sizes);
        currentRunMetrics = new ArrayList<>();
        for (int size : normalizedSizes) {
            System.out.println("==== RUN FOR SIZE: " + size + " ====");

            OperationMetric relWrite = runWriteTestRelational(size);
            OperationMetric mongoWrite = runWriteTestMongo(size);
            reportComparison("WRITE-" + size, relWrite, mongoWrite);
            OperationMetric mongoRefWrite = runWriteTestMongoRef(size);
            reportComparison("MONGO_REF_WRITE-" + size, mongoRefWrite, mongoWrite);

            List<OperationMetric> relFinds = runFindsRelational();
            List<OperationMetric> mongoFinds = runFindsMongo();
            compareMetricLists(relFinds, mongoFinds, "REL_VS_MONGO_");

            List<OperationMetric> mongoRefFinds = runFindsMongoRef();
            compareMetricLists(mongoRefFinds, mongoFinds, "MONGO_REF_VS_EMBED_");

            runAggregationComparison();
            runMongoIndexComparison();

            OperationMetric relUpdate = runUpdateRelational();
            OperationMetric mongoUpdate = runUpdateMongo();
            reportComparison("UPDATE", relUpdate, mongoUpdate);
            OperationMetric mongoRefUpdate = runUpdateMongoRef();
            reportComparison("MONGO_REF_UPDATE", mongoRefUpdate, mongoUpdate);

            OperationMetric relDelete = runDeleteRelational();
            OperationMetric mongoDelete = runDeleteMongo();
            reportComparison("DELETE", relDelete, mongoDelete);
            OperationMetric mongoRefDelete = runDeleteMongoRef();
            reportComparison("MONGO_REF_DELETE", mongoRefDelete, mongoDelete);

            System.out.println();
        }
        System.out.println("=== BENCHMARK DONE ===");
        BenchmarkSummary summary = new BenchmarkSummary(
                System.currentTimeMillis(),
                Arrays.stream(normalizedSizes).boxed().toList(),
                currentRunMetrics.stream().map(OperationMetric::toBenchmarkMetric).toList());
        this.lastSummary = summary;
        if (cloudBenchmarkClient != null) {
            cloudBenchmarkClient.compareWithRemote(summary);
        }
        return summary;
    }

    public BenchmarkSummary getLastSummary() {
        return lastSummary;
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
            return new OperationMetric("Relational", "UPDATE", 0, 0);
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
            return new OperationMetric("Relational", "DELETE", 0, 0);
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

    private OperationMetric runWriteTestMongoRef(int bookCount) {
        long duration = timeNs(() -> seedgenerator.seedMongoReferencing(bookCount));
        OperationMetric refMetric = logMetric("MongoRef", "WRITE_" + bookCount, duration, bookCount);
        return refMetric;
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
        List<TitleYearProjection> projection = bookRepoMongo.projectByTitle(FILTER_TERM);
        metrics.add(logMetric("Mongo", "FIND_FILTER_PROJECTION", System.nanoTime() - start, projection.size()));

        start = System.nanoTime();
        List<TitleYearProjection> projectionSorted = bookRepoMongo.projectSortedByTitle(FILTER_TERM);
        metrics.add(logMetric("Mongo", "FIND_FILTER_PROJECTION_SORT", System.nanoTime() - start, projectionSorted.size()));

        return metrics;
    }

    private List<OperationMetric> runFindsMongoRef() {
        List<OperationMetric> metrics = new ArrayList<>();

        long start = System.nanoTime();
        List<BookDocumentRef> allDocs = bookRepoMongoRef.findAll();
        metrics.add(logMetric("MongoRef", "FIND_ALL", System.nanoTime() - start, allDocs.size()));

        start = System.nanoTime();
        List<BookDocumentRef> filteredDocs = bookRepoMongoRef.findByTitleContainingIgnoreCase(FILTER_TERM);
        metrics.add(logMetric("MongoRef", "FIND_FILTER", System.nanoTime() - start, filteredDocs.size()));

        start = System.nanoTime();
        List<TitleYearProjection> projection = mapDocumentRefsToProjection(bookRepoMongoRef.findByTitleContainingIgnoreCase(FILTER_TERM));
        metrics.add(logMetric("MongoRef", "FIND_FILTER_PROJECTION", System.nanoTime() - start, projection.size()));

        start = System.nanoTime();
        List<TitleYearProjection> projectionSorted = sortProjectionDesc(projection);
        metrics.add(logMetric("MongoRef", "FIND_FILTER_PROJECTION_SORT", System.nanoTime() - start, projectionSorted.size()));
        return metrics;
    }

    private void runAggregationComparison() {
        Measured<List<PublisherAggregationResult>> relational = measure(bookRepo::aggregateByPublisher);
        OperationMetric relMetric = logMetric("Relational", "AGGREGATION",
                relational.durationNs(), relational.result().size());

        Measured<List<PublisherAggregationResult>> mongo = measure(bookRepoMongo::aggregateByPublisher);
        OperationMetric mongoMetric = logMetric("Mongo", "AGGREGATION",
                mongo.durationNs(), mongo.result().size());

        System.out.println("Relational Aggregation Beispiel: " + describeAggregation(relational.result()));
        System.out.println("Mongo Aggregation Beispiel: " + describeAggregation(mongo.result()));
        reportComparison("AGGREGATION", relMetric, mongoMetric);
    }

    private String describeAggregation(List<PublisherAggregationResult> aggregationResults) {
        return aggregationResults.stream().limit(3)
                .map(r -> r.getPublisherName() + " -> " + r.getBookCount() + " Bücher Ø " + r.getAverageReleaseYear())
                .collect(Collectors.joining(", "));
    }

    private void runMongoIndexComparison() {
        mongoIndexManager.dropIndexIfExists();
        List<OperationMetric> withoutIndex = runFindsMongo();

        mongoIndexManager.ensureIndex();
        List<OperationMetric> withIndex = runFindsMongo();

        compareMetricLists(withIndex, withoutIndex, "MONGO_INDEX_ON_VS_OFF_");
    }

    private OperationMetric runUpdateMongo() {
        List<BookDocument> documents = bookRepoMongo.findAll();
        if (documents.isEmpty()) {
            System.out.println("Mongo Update übersprungen – keine Daten vorhanden");
            return new OperationMetric("Mongo", "UPDATE", 0, 0);
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

    private OperationMetric runUpdateMongoRef() {
        List<BookDocumentRef> documents = bookRepoMongoRef.findAll();
        if (documents.isEmpty()) {
            System.out.println("MongoRef Update übersprungen – keine Daten vorhanden");
            return new OperationMetric("MongoRef", "UPDATE", 0, 0);
        }

        long duration = timeNs(() -> {
            documents.forEach(doc -> {
                doc.setReleaseYear(doc.getReleaseYear() + 1);
                doc.setIsbn(doc.getIsbn() + "-R");
            });
            bookRepoMongoRef.saveAll(documents);
        });
        return logMetric("MongoRef", "UPDATE", duration, documents.size());
    }

    @SuppressWarnings("null")
    private OperationMetric runDeleteMongo() {
        List<BookDocument> documents = bookRepoMongo.findAll();
        if (documents.isEmpty()) {
            System.out.println("Mongo Delete übersprungen – keine Daten vorhanden");
            return new OperationMetric("Mongo", "DELETE", 0, 0);
        }

        List<BookDocument> toDelete = documents.stream()
                .limit(Math.min(DELETE_BATCH, documents.size()))
                .collect(Collectors.toList());
        long duration = timeNs(() -> bookRepoMongo.deleteAll(toDelete));
        return logMetric("Mongo", "DELETE", duration, toDelete.size());
    }

    @SuppressWarnings("null")
    private OperationMetric runDeleteMongoRef() {
        List<BookDocumentRef> documents = bookRepoMongoRef.findAll();
        if (documents.isEmpty()) {
            System.out.println("MongoRef Delete übersprungen – keine Daten vorhanden");
            return new OperationMetric("MongoRef", "DELETE", 0, 0);
        }

        List<BookDocumentRef> toDelete = documents.stream()
                .limit(Math.min(DELETE_BATCH, documents.size()))
                .collect(Collectors.toList());
        long duration = timeNs(() -> bookRepoMongoRef.deleteAll(toDelete));
        return logMetric("MongoRef", "DELETE", duration, toDelete.size());
    }

    private void compareMetricLists(List<OperationMetric> first, List<OperationMetric> second, String labelPrefix) {
        int limit = Math.min(first.size(), second.size());
        for (int i = 0; i < limit; i++) {
            OperationMetric relMetric = first.get(i);
            OperationMetric mongoMetric = second.get(i);
            reportComparison(labelPrefix + relMetric.operation(), relMetric, mongoMetric);
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
        OperationMetric metric = new OperationMetric(datastore, operation, durationMs, affectedCount);
        currentRunMetrics.add(metric);
        return metric;
    }

    private long timeNs(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return System.nanoTime() - start;
    }

    private <T> Measured<T> measure(Supplier<T> supplier) {
        long start = System.nanoTime();
        T result = supplier.get();
        return new Measured<>(result, System.nanoTime() - start);
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

    private List<TitleYearProjection> mapDocumentRefsToProjection(List<BookDocumentRef> source) {
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
        private final String datastore;
        private final String operation;
        private final long durationMs;
        private final long affectedCount;

        private OperationMetric(String datastore, String operation, long durationMs, long affectedCount) {
            this.datastore = datastore;
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

        public BenchmarkMetric toBenchmarkMetric() {
            return new BenchmarkMetric(datastore, operation, durationMs, affectedCount);
        }
    }

    private static class Measured<T> {
        private final T result;
        private final long durationNs;

        private Measured(T result, long durationNs) {
            this.result = result;
            this.durationNs = durationNs;
        }

        T result() {
            return result;
        }

        long durationNs() {
            return durationNs;
        }
    }
}
