package com.example.dbi.BüchereiVerwaltung.Service;

import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class MongoIndexManager {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexManager.class);

    private final MongoTemplate mongoTemplate;
    private final boolean autoCreate;
    private final String indexField;
    private final String indexName;

    public MongoIndexManager(MongoTemplate mongoTemplate,
                             @Value("${app.mongo.index.auto-create:true}") boolean autoCreate,
                             @Value("${app.mongo.index.field:title}") String indexField) {
        this.mongoTemplate = mongoTemplate;
        this.autoCreate = autoCreate;
        this.indexField = (indexField == null || indexField.isBlank()) ? "title" : indexField;
        this.indexName = this.indexField + "_idx";
    }

    @PostConstruct
    public void init() {
        if (autoCreate) {
            ensureIndex();
        }
    }

    @SuppressWarnings("null")
    public void ensureIndex() {
        IndexOperations indexOps = mongoTemplate.indexOps(BookDocument.class);
        indexOps.ensureIndex(new Index().named(indexName).on(indexField, Sort.Direction.ASC));
        log.info("Mongo index {} on field '{}' ensured", indexName, indexField);
    }

    @SuppressWarnings("null")
    public void dropIndexIfExists() {
        IndexOperations indexOps = mongoTemplate.indexOps(BookDocument.class);
        try {
            indexOps.dropIndex(indexName);
            log.info("Mongo index {} dropped", indexName);
        } catch (DataAccessException ex) {
            log.debug("Index {} not present - nothing to drop", indexName);
        }
    }
}

