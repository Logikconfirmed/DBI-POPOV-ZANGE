package com.example.dbi.BüchereiVerwaltung.repositoriesMongo;

import com.example.dbi.BüchereiVerwaltung.dto.PublisherAggregationResult;
import com.example.dbi.BüchereiVerwaltung.dto.TitleYearProjection;
import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
class BookRepoMongoImpl implements BookRepoMongoCustom {

    private final MongoTemplate mongoTemplate;

    BookRepoMongoImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    @SuppressWarnings("null")
    public List<TitleYearProjection> projectByTitle(String term) {
        Query query = queryByTitle(term);
        return mongoTemplate.find(query, BookDocument.class).stream()
                .map(doc -> new TitleYearProjection(doc.getTitle(), doc.getReleaseYear()))
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("null")
    public List<TitleYearProjection> projectSortedByTitle(String term) {
        Query query = queryByTitle(term).with(Sort.by(Sort.Direction.DESC, "releaseYear"));
        return mongoTemplate.find(query, BookDocument.class).stream()
                .map(doc -> new TitleYearProjection(doc.getTitle(), doc.getReleaseYear()))
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("null")
    public List<PublisherAggregationResult> aggregateByPublisher() {
        TypedAggregation<BookDocument> aggregation = Aggregation.newAggregation(
                BookDocument.class,
                Aggregation.group("publisher.name")
                        .count().as("bookCount")
                        .avg("releaseYear").as("averageReleaseYear"),
                Aggregation.project("bookCount", "averageReleaseYear")
                        .and("_id").as("publisherName")
        );

        AggregationResults<Document> results =
                mongoTemplate.aggregate(aggregation, BookDocument.class, Document.class);
        return results.getMappedResults().stream()
                .map(doc -> {
                    String publisherName = doc.getString("publisherName");
                    if (publisherName == null) {
                        publisherName = "n/a";
                    }
                    Number bookCount = doc.get("bookCount", Number.class);
                    Number avgYear = doc.get("averageReleaseYear", Number.class);
                    return new PublisherAggregationResult(
                            publisherName,
                            bookCount == null ? 0L : bookCount.longValue(),
                            avgYear == null ? 0.0 : avgYear.doubleValue());
                })
                .collect(Collectors.toList());
    }

    private Query queryByTitle(String term) {
        String safeTerm = term == null ? "" : term;
        Criteria criteria = Criteria.where("title").regex(safeTerm, "i");
        Query query = new Query(criteria);
        query.fields().include("title").include("releaseYear");
        return query;
    }
}

