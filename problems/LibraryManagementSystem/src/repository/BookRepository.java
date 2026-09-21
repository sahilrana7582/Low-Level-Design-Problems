package repository;

import entities.Book;

import java.util.List;
import java.util.Optional;

public interface BookRepository {

    void save(Book book);

    Optional<Book> findById(String bookId);

    List<Book> findByTag(String tag);

    List<Book> findByAuthor(String author);
}
