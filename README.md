# DBI – Relationale vs. Mongo Persistenz (Benchmark & Bonusfeatures)

## Überblick
Dieses Projekt vergleicht eine relationale Spring-Boot-Anwendung (JPA/H2) mit zwei MongoDB-Varianten:

1. **Embedded JSON** – Daten werden mit eingebetteten Publisher-/Author-Dokumenten gespeichert.
2. **Referencing** – Bücher referenzieren Publisher/Author via `@DBRef`.

Ein Benchmark (`LibraryPerformanceTester`) misst CRUD-, Aggregations- und Index-Szenarien für alle Stores, stellt die Ergebnisse als API/CommandLineRunner bereit und speichert sie als `BenchmarkSummary`. Zusätzlich existieren ein CRUD-Web-Frontend für Mongo (embedded) sowie JSON-Schema-Validierung inkl. negativer Tests.

## Projekt-Setup

### Voraussetzungen
- JDK 17
- Maven Wrapper (`./mvnw`) oder Maven
- Laufender MongoDB-Server auf `mongodb://localhost:27017`
- Optional: Weitere Instanz (Cloud), die dieselbe Anwendung bereitstellt, wenn der Cloud-Vergleich genutzt werden soll

### Lokalen Build/Test ausführen
```bash
./mvnw clean test
```
> Hinweis: Dadurch laufen u.a. `LibraryPerformanceTesterIntegrationTest` (führt den Benchmark mit kleinen Datenmengen aus) und `MongoSchemaValidatorTest` (prüft JSON-Schema-Konformität).

### Anwendung starten
```bash
./mvnw spring-boot:run
```
- Der CommandLineRunner startet automatisch den Benchmark mit den Standardgrößen (100, 1000).  
- Soll der Benchmark nicht automatisch ausgeführt werden (z.B. für UI-Tests), kann in `application.yml` bzw. per Umgebungsvariable `app.runner.enabled=false` gesetzt werden.

## Konfiguration

`src/main/resources/application.yml` enthält:
- H2-In-Memory-Datenbank (`jdbc:h2:mem:buecherei`) mit `ddl-auto:update`.
- Lokale MongoDB-URI.
- `app.runner.enabled` – steuert, ob der Benchmark beim Start läuft.
- `app.mongo.index.*` – automatisches Erstellen eines Index (Default: `title`).

Für Tests (`application-test.yml`):
- Embedded Mongo wird von Flapdoodle provisioniert.
- Benchmark-Runner bleibt deaktiviert.

## Benchmark & Messungen

### CommandLineRunner
`LibraryPerformanceTester.runBenchmark(int... sizes)` wird beim Start ausgeführt und schreibt pro Größe:
- **Write-Tests** (Relational, Mongo, MongoRef)
- **4 Read-Szenarien** pro Store (FindAll, Filter, Filter+Projektion, Filter+Projektion+Sort)
- **Referencing vs. Embedded Vergleich**
- **Aggregation (Publisher -> Anzahl, Ø-Jahr)** auf beiden Stores
- **Index On/Off Vergleich** (Mongo)
- **Update** und **Delete** auf allen Stores

Ergebnisse erscheinen in der Konsole und werden als `BenchmarkSummary` im Service gehalten. Optional wird derselbe Lauf mit einer Remote-Instanz verglichen (siehe Cloud-Vergleich).

### API
`BenchmarkController`
- `POST /api/benchmark/run` – triggert Benchmark erneut; Body `{ "sizes": [100, 1000] }` optional.
- `GET /api/benchmark/summary` – liefert letzte Messwerte (`BenchmarkSummary`).

### Cloud-Vergleich
`CloudBenchmarkClient` ruft (falls `app.cloud.remote-url` gesetzt ist) `GET {remote}/api/benchmark/summary` auf und loggt Laufzeitdifferenzen für identische Operationen.

## Mongo CRUD-Frontend
Pfad: `http://localhost:8080/mongo/books`
- Liste vorhandener Bücher (embedded Mongo) mit Lösch- und Bearbeiten-Buttons.
- Formular zum Erstellen neuer Bücher (Titel, ISBN, Jahr, Publisher, Autoren).
- Edit-Ansicht + Delete.

Verantwortliche Klassen:
- `MongoBookController`, `MongoBookService`, `BookForm`
- Templates unter `src/main/resources/templates/books/`
- CSS unter `src/main/resources/static/css/books.css`

## JSON-Schema & Validierung
- Schema definiert zwingende Felder (Titel, ISBN, releaseYear, Publisher + Address, Authors).
- `MongoSchemaValidator` prüft jedes Dokument vor dem Speichern (Seed + Frontend + Tests).
- Tests (`MongoSchemaValidatorTest`) decken positive/negative Fälle ab (z.B. fehlende Autoren).

## Referencing-Modell
- `modelMongo/referencing` enthält `BookDocumentRef`, `AuthorDocumentRef`, `PublisherDocumentRef` + zugehörige Repositories.
- `Seedgenerator.seedMongoReferencing` befüllt die Referenz-Struktur; Benchmark ruft alle CRUD-Operationen auch dafür auf.

## Index-Vergleich
- `MongoIndexManager` kann den Titel-Index zur Laufzeit droppen/neu anlegen.
- `runMongoIndexComparison()` fährt nacheinander Tests ohne/mit Index und vergleicht Laufzeiten.

## Aggregationen (Bonus)
- Relationale Aggregation: `BookRepositoryJpa.aggregateByPublisher` (JPQL).
- Mongo Aggregation: `BookRepoMongoImpl.aggregateByPublisher` (Pipeline über MongoTemplate).
- Benchmark protokolliert beide Ergebnisse inklusive Beispielausgabe.

## Bonus-Feature-Checkliste

| Feature | Status | Nachweis |
| --- | --- | --- |
| Pflicht Teil 1 (rel. Projekt + skalierbarer Seed) | ✅ | `Seedgenerator.seedRelational`, JPA-Modelle |
| Pflicht Teil 2 (Mongo + JSON Modell) | ✅ | `BookDocument` + `Seedgenerator.seedMongo` |
| Pflicht Teil 3 (CRUD-Benchmark inkl. Tracking) | ✅ | `LibraryPerformanceTester`, CLI-Ausgabe, `LibraryPerformanceTesterIntegrationTest` |
| Aggregations-Query + Vergleich | ✅ | `BookRepositoryJpa.aggregateByPublisher`, `BookRepoMongoImpl.aggregateByPublisher`, Benchmark-Ausgabe |
| Referencing-Variante + Vergleich | ✅ | `modelMongo/referencing`, Seeds, Benchmark-Vergleiche (`MONGO_REF_*`) |
| Cloud-Laufzeitvergleich | ✅ | `CloudBenchmarkClient`, `BenchmarkController` REST |
| JSON-Schema + Tests | ✅ | `schemas/book-document-schema.json`, `MongoSchemaValidator`, Testklasse |
| CRUD-Frontend (nicht Swagger) | ✅ | `/mongo/books`, Thymeleaf-Views |
| Index-Vergleich Mongo | ✅ | `MongoIndexManager`, `runMongoIndexComparison()` |

## Tipps zum Testen
1. Stelle sicher, dass MongoDB lokal läuft.
2. `./mvnw test` – führt alle automatischen Tests aus.
3. `./mvnw spring-boot:run` – beobachte Benchmark-Konsolenausgabe.
4. Greife auf das Frontend zu (`/mongo/books`) und teste CRUD.
5. Probiere `/api/benchmark/run` (z.B. via cURL oder REST-Client) für manuelle Messungen.
6. Optional: Cloud-Instanz starten, Benchmark dort laufen lassen und lokal `app.cloud.remote-url` auf die Remote-URL setzen; beim nächsten lokalen Benchmark erscheinen Vergleichslogs.

Viel Erfolg beim Präsentieren und Weiterentwickeln! 🎯

