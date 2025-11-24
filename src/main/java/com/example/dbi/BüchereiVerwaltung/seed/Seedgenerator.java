package com.example.dbi.BüchereiVerwaltung.seed;

import com.example.dbi.BüchereiVerwaltung.model.Author;
import com.example.dbi.BüchereiVerwaltung.model.Book;
import com.example.dbi.BüchereiVerwaltung.model.Publisher;
import com.example.dbi.BüchereiVerwaltung.modelMongo.AuthorEmbedded;
import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import com.example.dbi.BüchereiVerwaltung.modelMongo.PublisherEmbedded;
import com.example.dbi.BüchereiVerwaltung.repositories.AuthorRepositoryJpa;
import com.example.dbi.BüchereiVerwaltung.repositories.BookRepositoryJpa;
import com.example.dbi.BüchereiVerwaltung.repositories.PublisherRepositoryJpa;
import com.example.dbi.BüchereiVerwaltung.repositoriesMongo.BookRepoMongo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class Seedgenerator {

    private final PublisherRepositoryJpa publisherRepo;
    private final AuthorRepositoryJpa authorRepo;
    private final BookRepositoryJpa bookRepo;
    private final BookRepoMongo bookRepoMongo;
    private final Random random = new Random();

    public Seedgenerator(PublisherRepositoryJpa publisherRepo,
                         AuthorRepositoryJpa authorRepo,
                         BookRepositoryJpa bookRepo,
                         BookRepoMongo bookRepoMongo) {
        this.publisherRepo = publisherRepo;
        this.authorRepo = authorRepo;
        this.bookRepo = bookRepo;
        this.bookRepoMongo = bookRepoMongo;
    }

    @SuppressWarnings("null")
    public void seedRelational(int bookCount) {
        // clean existing data first
        bookRepo.deleteAll();
        authorRepo.deleteAll();
        publisherRepo.deleteAll();

        List<Publisher> publishers = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new Publisher("Publisher " + i, "Address " + i))
                .collect(Collectors.toCollection(ArrayList::new));
        List<Publisher> savedPublishers = publisherRepo.saveAll(publishers);

        List<Author> authors = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Author("Author " + i))
                .collect(Collectors.toCollection(ArrayList::new));
        List<Author> savedAuthors = authorRepo.saveAll(authors);

        List<Book> books = new ArrayList<>();
        for (int i = 1; i <= bookCount; i++) {
            Publisher publisher = savedPublishers.get(random.nextInt(savedPublishers.size()));
            List<Author> bookAuthors = randomAuthors(savedAuthors);

            Book book = new Book("Book " + i, 2000 + random.nextInt(25), publisher, bookAuthors);
            publisher.getBooks().add(book);
            books.add(book);
        }

        bookRepo.saveAll(books);
    }

    @SuppressWarnings("null")
    public void seedMongo(int bookCount) {
        bookRepoMongo.deleteAll();

        List<BookDocument> documents = IntStream.rangeClosed(1, bookCount)
                .mapToObj(i -> new BookDocument(
                        "Book " + i,
                        "ISBN-M-" + i,
                        randomPublisherEmbedded(),
                        2000 + random.nextInt(25),
                        randomAuthorEmbeds()))
                .collect(Collectors.toList());

        bookRepoMongo.saveAll(documents);
    }

    private List<Author> randomAuthors(List<Author> pool) {
        int authorCount = 1 + random.nextInt(3);
        Set<Author> selected = new HashSet<>();
        while (selected.size() < authorCount) {
            selected.add(pool.get(random.nextInt(pool.size())));
        }
        return new ArrayList<>(selected);
    }

    private List<AuthorEmbedded> randomAuthorEmbeds() {
        int authorCount = 1 + random.nextInt(3);
        List<AuthorEmbedded> result = new ArrayList<>();
        for (int i = 0; i < authorCount; i++) {
            result.add(new AuthorEmbedded("Author M " + random.nextInt(50)));
        }
        return result;
    }

    private PublisherEmbedded randomPublisherEmbedded() {
        return new PublisherEmbedded(
                "Publisher M " + random.nextInt(20),
                "Mongo Street " + random.nextInt(100));
    }
}