package com.example.dbi.BüchereiVerwaltung.modelMongo;

public class AuthorEmbedded {
    private String name;
    public AuthorEmbedded(){}
    public AuthorEmbedded(String name){ this.name = name; }
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
}
