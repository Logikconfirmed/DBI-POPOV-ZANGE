package com.example.dbi.BüchereiVerwaltung.repositories;

import com.example.dbi.BüchereiVerwaltung.dto.PublisherAggregationResult;
import com.example.dbi.BüchereiVerwaltung.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepositoryJpa extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String term);

    @Query("SELECT new com.example.dbi.BüchereiVerwaltung.dto.PublisherAggregationResult(" +
            "b.publisher.name, COUNT(b), AVG(b.releaseYear)) " +
            "FROM Book b GROUP BY b.publisher.name")
    List<PublisherAggregationResult> aggregateByPublisher();
}
