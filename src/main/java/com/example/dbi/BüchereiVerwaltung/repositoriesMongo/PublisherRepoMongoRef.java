package com.example.dbi.BüchereiVerwaltung.repositoriesMongo;

import com.example.dbi.BüchereiVerwaltung.modelMongo.referencing.PublisherDocumentRef;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PublisherRepoMongoRef extends MongoRepository<PublisherDocumentRef, String> {
}

