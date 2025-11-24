package com.example.dbi.BüchereiVerwaltung.repositoriesMongo;

import com.example.dbi.BüchereiVerwaltung.modelMongo.referencing.AuthorDocumentRef;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthorRepoMongoRef extends MongoRepository<AuthorDocumentRef, String> {
}

