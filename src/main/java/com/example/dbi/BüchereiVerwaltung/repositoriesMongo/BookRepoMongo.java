package com.example.dbi.BüchereiVerwaltung.repositoriesMongo;

import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookRepoMongo extends MongoRepository<BookDocument, String>, BookRepoMongoCustom {
    List<BookDocument> findByTitleContainingIgnoreCase(String term);
}
