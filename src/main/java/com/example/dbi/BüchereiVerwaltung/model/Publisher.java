package com.example.dbi.BüchereiVerwaltung.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publishers")
public class Publisher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String address;

    @OneToMany(mappedBy = "publisher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    public Publisher(){}

    public Publisher(Long id, String name, String address, List<Book> books) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.books = books;
    }

    public Publisher(String name) {
        this(name, "Unknown address");
    }

    public Publisher(String name, String address) {
        this.name = name;
        this.address = address;
    }

    // Getter/Setter
    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public String getAddress(){ return address; }
    public void setAddress(String address){ this.address = address; }
    public List<Book> getBooks(){ return books; }
    public void setBooks(List<Book> books){ this.books = books; }


}

