package com.example.dbi;

import com.example.dbi.BüchereiVerwaltung.modelMongo.AuthorEmbedded;
import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import com.example.dbi.BüchereiVerwaltung.modelMongo.PublisherEmbedded;
import com.example.dbi.BüchereiVerwaltung.validation.MongoSchemaValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MongoSchemaValidatorTest {

    @Autowired
    private MongoSchemaValidator validator;

    @Test
    void acceptsValidDocument() {
        BookDocument document = new BookDocument(
                "Schema Book",
                "ISBN-TEST-1",
                new PublisherEmbedded("Schema Publisher", "Schema Street"),
                2020,
                List.of(new AuthorEmbedded("Schema Author"))
        );

        assertThatCode(() -> validator.validate(document)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidDocumentWithoutAuthors() {
        BookDocument document = new BookDocument(
                "Schema Book",
                "ISBN-TEST-1",
                new PublisherEmbedded("Schema Publisher", "Schema Street"),
                2020,
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mongo Document verletzt Schema");
    }
}

