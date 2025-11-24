package com.example.dbi.BüchereiVerwaltung.repositories;

import com.example.dbi.BüchereiVerwaltung.model.Book;
import com.example.dbi.BüchereiVerwaltung.dto.TitleYearProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookRepositoryJpa extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String term);

    @Query("select b.title as title, b.releaseYear as releaseYear from Book b " +
            "where lower(b.title) like lower(concat('%', :term, '%'))")
    List<TitleYearProjection> findProjectedByTitle(@Param("term") String term);

    @Query("select b.title as title, b.releaseYear as releaseYear from Book b " +
            "where lower(b.title) like lower(concat('%', :term, '%')) order by b.releaseYear desc")
    List<TitleYearProjection> findProjectedSortedByTitle(@Param("term") String term);
}
