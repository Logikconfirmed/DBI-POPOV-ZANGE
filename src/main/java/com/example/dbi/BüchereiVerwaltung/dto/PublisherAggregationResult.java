package com.example.dbi.BüchereiVerwaltung.dto;

public class PublisherAggregationResult {
    private final String publisherName;
    private final long bookCount;
    private final double averageReleaseYear;

    public PublisherAggregationResult(String publisherName, long bookCount, double averageReleaseYear) {
        this.publisherName = publisherName;
        this.bookCount = bookCount;
        this.averageReleaseYear = averageReleaseYear;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public long getBookCount() {
        return bookCount;
    }

    public double getAverageReleaseYear() {
        return averageReleaseYear;
    }
}

