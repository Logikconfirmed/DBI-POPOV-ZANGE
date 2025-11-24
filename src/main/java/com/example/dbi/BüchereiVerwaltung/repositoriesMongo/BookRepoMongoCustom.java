package com.example.dbi.BüchereiVerwaltung.repositoriesMongo;

import com.example.dbi.BüchereiVerwaltung.dto.PublisherAggregationResult;
import com.example.dbi.BüchereiVerwaltung.dto.TitleYearProjection;

import java.util.List;

public interface BookRepoMongoCustom {
    List<TitleYearProjection> projectByTitle(String term);

    List<TitleYearProjection> projectSortedByTitle(String term);

    List<PublisherAggregationResult> aggregateByPublisher();
}

