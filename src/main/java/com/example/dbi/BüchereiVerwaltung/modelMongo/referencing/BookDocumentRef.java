package com.example.dbi.BüchereiVerwaltung.modelMongo.referencing;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "books_ref")
public class BookDocumentRef {
    @Id
    private String id;
    private String title;
    private String isbn;
    private int releaseYear;

    @DBRef
    private PublisherDocumentRef publisher;

    @DBRef
    private List<AuthorDocumentRef> authors = new ArrayList<>();

    public BookDocumentRef() {
    }

    public BookDocumentRef(String title, String isbn, int releaseYear, PublisherDocumentRef publisher,
                           List<AuthorDocumentRef> authors) {
        this.title = title;
        this.isbn = isbn;
        this.releaseYear = releaseYear;
        this.publisher = publisher;
        this.authors = authors;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public PublisherDocumentRef getPublisher() {
        return publisher;
    }

    public void setPublisher(PublisherDocumentRef publisher) {
        this.publisher = publisher;
    }

    public List<AuthorDocumentRef> getAuthors() {
        return authors;
    }

    public void setAuthors(List<AuthorDocumentRef> authors) {
        this.authors = authors;
    }
}

