package com.example.dbi.BüchereiVerwaltung.repositoriesMongo;

import com.example.dbi.BüchereiVerwaltung.modelMongo.referencing.BookDocumentRef;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookRepoMongoRef extends MongoRepository<BookDocumentRef, String> {
    List<BookDocumentRef> findByTitleContainingIgnoreCase(String term);
}

