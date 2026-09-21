package repository;

import entities.Book;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryBookRepository implements BookRepository {

    private final Map<String, Book> bookMap = new HashMap<>();
    private final Map<String, List<Book>> bookByTags = new HashMap<>();
    private final Map<String, List<Book>> bookByAuthor = new HashMap<>();

    private final ReentrantReadWriteLock lock =
            new ReentrantReadWriteLock();

    @Override
    public void save(Book book) {

        var writeLock = lock.writeLock();
        writeLock.lock();

        try {

            Book oldBook =
                    bookMap.get(book.getId());

            // Remove old indexes if this book already exists
            if (oldBook != null) {

                bookByAuthor
                        .getOrDefault(
                                oldBook.getAuthor(),
                                List.of()
                        )
                        .remove(oldBook);

                for (String tag : oldBook.getTags()) {

                    List<Book> books =
                            bookByTags.get(tag);

                    if (books != null) {
                        books.remove(oldBook);

                        if (books.isEmpty()) {
                            bookByTags.remove(tag);
                        }
                    }
                }
            }

            // Replace/store the book
            bookMap.put(
                    book.getId(),
                    book
            );

            // Add new author index
            bookByAuthor
                    .computeIfAbsent(
                            book.getAuthor(),
                            k -> new ArrayList<>()
                    )
                    .add(book);

            // Add new tag indexes
            for (String tag : book.getTags()) {

                bookByTags
                        .computeIfAbsent(
                                tag,
                                k -> new ArrayList<>()
                        )
                        .add(book);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<Book> findById(String bookId) {
        var readLock = lock.readLock();
        readLock.lock();

        try {
            return Optional.ofNullable(bookMap.get(bookId));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Book> findByTag(String tag) {
        var readLock = lock.readLock();
        readLock.lock();

        try {
            return List.copyOf(
                    bookByTags.getOrDefault(tag, List.of())
            );
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Book> findByAuthor(String author) {
        var readLock = lock.readLock();
        readLock.lock();

        try {
            return List.copyOf(
                    bookByAuthor.getOrDefault(author, List.of())
            );
        } finally {
            readLock.unlock();
        }
    }
}