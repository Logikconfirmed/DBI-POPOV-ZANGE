package com.example.dbi.BüchereiVerwaltung.modelMongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "books")
public class BookDocument {
    @Id
    private String id;
    private String title;
    private String isbn;
    private PublisherEmbedded publisher;
    private int releaseYear;
    private List<AuthorEmbedded> authors;

    public BookDocument(){}

    public BookDocument(String title, String isbn, PublisherEmbedded publisher, int releaseYear, List<AuthorEmbedded> authors){
        this.title = title;
        this.isbn = isbn;
        this.publisher = publisher;
        this.releaseYear = releaseYear;
        this.authors = authors;
    }

    // getter/setter...
    public String getId(){ return id; }
    public void setId(String id){ this.id = id; }
    public String getTitle(){ return title; }
    public void setTitle(String title){ this.title = title; }
    public String getIsbn(){ return isbn; }
    public void setIsbn(String isbn){ this.isbn = isbn; }
    public PublisherEmbedded getPublisher(){ return publisher; }
    public void setPublisher(PublisherEmbedded publisher){ this.publisher = publisher; }
    public int getReleaseYear() { return releaseYear; }
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; }
    public List<AuthorEmbedded> getAuthors(){ return authors; }
    public void setAuthors(List<AuthorEmbedded> authors){ this.authors = authors; }
}
