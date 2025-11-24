package com.example.dbi.BüchereiVerwaltung.repositories;

import com.example.dbi.BüchereiVerwaltung.model.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepositoryJpa extends JpaRepository<Publisher, Long> {}

