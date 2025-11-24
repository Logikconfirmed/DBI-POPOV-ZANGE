package com.example.dbi.BüchereiVerwaltung.dto;

/**
 * Simple DTO used to represent read projections (title + releaseYear).
 */
public class TitleYearProjection {
    private final String title;
    private final int releaseYear;

    public TitleYearProjection(String title, int releaseYear) {
        this.title = title;
        this.releaseYear = releaseYear;
    }

    public String getTitle() {
        return title;
    }

    public int releaseYear() {
        return releaseYear;
    }
}

