package repository;

import exceptions.BookNotFoundException;
import exceptions.NoCopiesAvailableException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryInventoryRepository
        implements InventoryRepository {

    private final Map<String, AtomicInteger> bookQuantity =
            new HashMap<>();

    private final ReentrantReadWriteLock lock =
            new ReentrantReadWriteLock();

    @Override
    public void addStock(String bookId, int quantity) {
        var writeLock = lock.writeLock();
        writeLock.lock();

        try {
            bookQuantity
                    .computeIfAbsent(bookId, k -> new AtomicInteger())
                    .addAndGet(quantity);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean isAvailable(String bookId) {
        var readLock = lock.readLock();
        readLock.lock();

        try {
            AtomicInteger quantity = bookQuantity.get(bookId);
            return quantity != null && quantity.get() > 0;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void decrement(String bookId) {
        var writeLock = lock.writeLock();
        writeLock.lock();

        try {
            AtomicInteger quantity = bookQuantity.get(bookId);

            if (quantity == null || quantity.get() <= 0) {
                throw new NoCopiesAvailableException(bookId);
            }

            quantity.decrementAndGet();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void increment(String bookId) {
        var writeLock = lock.writeLock();
        writeLock.lock();

        try {
            AtomicInteger quantity = bookQuantity.get(bookId);

            if (quantity == null) {
                throw new BookNotFoundException(bookId);
            }

            quantity.incrementAndGet();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int getQuantity(String bookId) {
        var readLock = lock.readLock();
        readLock.lock();

        try {
            AtomicInteger quantity = bookQuantity.get(bookId);
            return quantity == null ? 0 : quantity.get();
        } finally {
            lock.readLock().unlock();
        }
    }
}
