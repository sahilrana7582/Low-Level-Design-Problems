package repository;

import entities.Booking;

import java.util.*;

public interface BookingRepository {

    void save(Booking booking);

    Optional<Booking> findById(String bookingId);

    List<Booking> findByUser(String userId);
}