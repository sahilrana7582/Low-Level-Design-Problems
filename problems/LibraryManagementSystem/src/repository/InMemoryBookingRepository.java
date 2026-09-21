package repository;

import entities.Booking;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryBookingRepository
        implements BookingRepository {

    private final Map<String, Booking> bookingMap = new HashMap<>();

    private final Map<String, List<Booking>> bookingByUser =
            new HashMap<>();

    private final ReentrantReadWriteLock lock =
            new ReentrantReadWriteLock();

    @Override
    public void save(Booking booking) {
        var writeLock = lock.writeLock();
        writeLock.lock();

        try {
            bookingMap.put(booking.getId(), booking);

            bookingByUser
                    .computeIfAbsent(
                            booking.getUser().getId(),
                            k -> new ArrayList<>()
                    )
                    .add(booking);

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        var readLock = lock.readLock();
        readLock.lock();

        try {
            return Optional.ofNullable(
                    bookingMap.get(bookingId)
            );
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Booking> findByUser(String userId) {
        var readLock = lock.readLock();
        readLock.lock();

        try {
            return List.copyOf(
                    bookingByUser.getOrDefault(
                            userId,
                            List.of()
                    )
            );
        } finally {
            readLock.unlock();
        }
    }
}
