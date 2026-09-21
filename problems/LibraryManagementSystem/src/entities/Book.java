package entities;

import java.time.LocalDateTime;
import java.util.List;

public final class Book {

    private final String id;
    private final String name;
    private final String author;
    private final String isbn;
    private final List<String> tags;
    private final LocalDateTime publishedDate;

    public Book(
            String id,
            String name,
            String author,
            String isbn,
            List<String> tags,
            LocalDateTime publishedDate
    ) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.isbn = isbn;
        this.tags = List.copyOf(tags);
        this.publishedDate = publishedDate;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public List<String> getTags() {
        return tags;
    }

    public LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", isbn='" + isbn + '\'' +
                ", tags=" + tags +
                ", publishedDate=" + publishedDate +
                '}';
    }
}