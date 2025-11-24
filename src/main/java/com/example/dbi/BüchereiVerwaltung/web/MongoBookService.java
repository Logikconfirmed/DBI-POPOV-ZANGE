package com.example.dbi.BüchereiVerwaltung.web;

import com.example.dbi.BüchereiVerwaltung.modelMongo.AuthorEmbedded;
import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import com.example.dbi.BüchereiVerwaltung.modelMongo.PublisherEmbedded;
import com.example.dbi.BüchereiVerwaltung.repositoriesMongo.BookRepoMongo;
import com.example.dbi.BüchereiVerwaltung.validation.MongoSchemaValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MongoBookService {

    private final BookRepoMongo bookRepoMongo;
    private final MongoSchemaValidator schemaValidator;

    public MongoBookService(BookRepoMongo bookRepoMongo, MongoSchemaValidator schemaValidator) {
        this.bookRepoMongo = bookRepoMongo;
        this.schemaValidator = schemaValidator;
    }

    public List<BookDocument> listAll() {
        return bookRepoMongo.findAll();
    }

    @SuppressWarnings("null")
    public BookDocument create(BookForm form) {
        BookDocument document = formToDocument(new BookDocument(), form);
        schemaValidator.validate(document);
        return bookRepoMongo.save(document);
    }

    @SuppressWarnings("null")
    public BookDocument update(String id, BookForm form) {
        BookDocument existing = findById(id);
        BookDocument document = formToDocument(existing, form);
        schemaValidator.validate(document);
        return bookRepoMongo.save(document);
    }

    @SuppressWarnings("null")
    public BookDocument findById(String id) {
        return bookRepoMongo.findById(requireText(id, "ID"))
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + id));
    }

    @SuppressWarnings("null")
    public void delete(String id) {
        bookRepoMongo.deleteById(requireText(id, "ID"));
    }

    public BookForm toForm(BookDocument document) {
        BookForm form = new BookForm();
        form.setTitle(document.getTitle());
        form.setIsbn(document.getIsbn());
        form.setReleaseYear(document.getReleaseYear());
        if (document.getPublisher() != null) {
            form.setPublisherName(document.getPublisher().getName());
            form.setPublisherAddress(document.getPublisher().getAddress());
        }
        form.setAuthors(document.getAuthors().stream()
                .map(AuthorEmbedded::getName)
                .collect(Collectors.joining(", ")));
        return form;
    }

    private BookDocument formToDocument(BookDocument target, BookForm form) {
        target.setTitle(requireText(form.getTitle(), "Titel"));
        target.setIsbn(requireText(form.getIsbn(), "ISBN"));
        target.setReleaseYear(form.getReleaseYear());
        target.setPublisher(new PublisherEmbedded(
                requireText(form.getPublisherName(), "Publisher Name"),
                requireText(form.getPublisherAddress(), "Publisher Adresse")
        ));
        target.setAuthors(parseAuthors(form.getAuthors()));
        return target;
    }

    private List<AuthorEmbedded> parseAuthors(String authors) {
        if (!StringUtils.hasText(authors)) {
            throw new IllegalArgumentException("Mindestens ein Autor muss angegeben werden");
        }
        return Arrays.stream(authors.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(AuthorEmbedded::new)
                .collect(Collectors.toList());
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " darf nicht leer sein");
        }
        return value;
    }
}

