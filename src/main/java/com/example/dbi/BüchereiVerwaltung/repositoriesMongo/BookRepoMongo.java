package com.example.dbi.BüchereiVerwaltung.repositoriesMongo;


import com.example.dbi.BüchereiVerwaltung.dto.TitleYearProjection;
import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface BookRepoMongo extends MongoRepository<BookDocument, String> {
    List<BookDocument> findByTitleContainingIgnoreCase(String term);

    @Query(value = "{ 'title': { $regex: ?0, $options: 'i' } }",
            fields = "{ 'title': 1, 'releaseYear': 1, '_id': 0 }")
    List<TitleYearProjection> findProjectedByTitle(String term);

    @Query(value = "{ 'title': { $regex: ?0, $options: 'i' } }",
            fields = "{ 'title': 1, 'releaseYear': 1, '_id': 0 }",
            sort = "{ 'releaseYear': -1 }")
    List<TitleYearProjection> findProjectedSortedByTitle(String term);
}
