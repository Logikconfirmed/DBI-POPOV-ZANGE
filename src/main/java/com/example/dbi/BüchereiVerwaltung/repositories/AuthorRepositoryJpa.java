package com.example.dbi.BüchereiVerwaltung.repositories;

import com.example.dbi.BüchereiVerwaltung.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepositoryJpa extends JpaRepository<Author, Long> {}
