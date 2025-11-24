package com.example.dbi.BüchereiVerwaltung.seed;

import com.example.dbi.BüchereiVerwaltung.model.Author;
import com.example.dbi.BüchereiVerwaltung.model.Book;
import com.example.dbi.BüchereiVerwaltung.model.Publisher;
import com.example.dbi.BüchereiVerwaltung.modelMongo.AuthorEmbedded;
import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import com.example.dbi.BüchereiVerwaltung.modelMongo.PublisherEmbedded;
import com.example.dbi.BüchereiVerwaltung.modelMongo.referencing.AuthorDocumentRef;
import com.example.dbi.BüchereiVerwaltung.modelMongo.referencing.BookDocumentRef;
import com.example.dbi.BüchereiVerwaltung.modelMongo.referencing.PublisherDocumentRef;
import com.example.dbi.BüchereiVerwaltung.repositories.AuthorRepositoryJpa;
import com.example.dbi.BüchereiVerwaltung.repositories.BookRepositoryJpa;
import com.example.dbi.BüchereiVerwaltung.repositories.PublisherRepositoryJpa;
import com.example.dbi.BüchereiVerwaltung.repositoriesMongo.AuthorRepoMongoRef;
import com.example.dbi.BüchereiVerwaltung.repositoriesMongo.BookRepoMongo;
import com.example.dbi.BüchereiVerwaltung.repositoriesMongo.BookRepoMongoRef;
import com.example.dbi.BüchereiVerwaltung.repositoriesMongo.PublisherRepoMongoRef;
import com.example.dbi.BüchereiVerwaltung.validation.MongoSchemaValidator;
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
    private final BookRepoMongoRef bookRepoMongoRef;
    private final AuthorRepoMongoRef authorRepoMongoRef;
    private final PublisherRepoMongoRef publisherRepoMongoRef;
    private final MongoSchemaValidator schemaValidator;
    private final Random random = new Random();

    public Seedgenerator(PublisherRepositoryJpa publisherRepo,
                         AuthorRepositoryJpa authorRepo,
                         BookRepositoryJpa bookRepo,
                         BookRepoMongo bookRepoMongo,
                         BookRepoMongoRef bookRepoMongoRef,
                         AuthorRepoMongoRef authorRepoMongoRef,
                         PublisherRepoMongoRef publisherRepoMongoRef,
                         MongoSchemaValidator schemaValidator) {
        this.publisherRepo = publisherRepo;
        this.authorRepo = authorRepo;
        this.bookRepo = bookRepo;
        this.bookRepoMongo = bookRepoMongo;
        this.bookRepoMongoRef = bookRepoMongoRef;
        this.authorRepoMongoRef = authorRepoMongoRef;
        this.publisherRepoMongoRef = publisherRepoMongoRef;
        this.schemaValidator = schemaValidator;
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
                .mapToObj(i -> {
                    BookDocument document = new BookDocument(
                            "Book " + i,
                            "ISBN-M-" + i,
                            randomPublisherEmbedded(),
                            2000 + random.nextInt(25),
                            randomAuthorEmbeds());
                    schemaValidator.validate(document);
                    return document;
                })
                .collect(Collectors.toList());

        bookRepoMongo.saveAll(documents);
    }

    @SuppressWarnings("null")
    public void seedMongoReferencing(int bookCount) {
        bookRepoMongoRef.deleteAll();
        authorRepoMongoRef.deleteAll();
        publisherRepoMongoRef.deleteAll();

        List<PublisherDocumentRef> publishers = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new PublisherDocumentRef("PublisherRef " + i, "Cloud Street " + i))
                .collect(Collectors.toList());
        List<PublisherDocumentRef> savedPublishers = publisherRepoMongoRef.saveAll(publishers);

        List<AuthorDocumentRef> authors = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new AuthorDocumentRef("AuthorRef " + i))
                .collect(Collectors.toList());
        List<AuthorDocumentRef> savedAuthors = authorRepoMongoRef.saveAll(authors);

        List<BookDocumentRef> books = new ArrayList<>();
        for (int i = 1; i <= bookCount; i++) {
            PublisherDocumentRef publisher = savedPublishers.get(random.nextInt(savedPublishers.size()));
            List<AuthorDocumentRef> bookAuthors = randomAuthorRefs(savedAuthors);
            BookDocumentRef doc = new BookDocumentRef(
                    "BookRef " + i,
                    "ISBN-REF-" + i,
                    2000 + random.nextInt(25),
                    publisher,
                    bookAuthors
            );
            books.add(doc);
        }

        bookRepoMongoRef.saveAll(books);
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

    private List<AuthorDocumentRef> randomAuthorRefs(List<AuthorDocumentRef> pool) {
        int authorCount = 1 + random.nextInt(3);
        Set<AuthorDocumentRef> selected = new HashSet<>();
        while (selected.size() < authorCount) {
            selected.add(pool.get(random.nextInt(pool.size())));
        }
        return new ArrayList<>(selected);
    }

    private PublisherEmbedded randomPublisherEmbedded() {
        return new PublisherEmbedded(
                "Publisher M " + random.nextInt(20),
                "Mongo Street " + random.nextInt(100));
    }
}