import entity.HotelInventory;
import entity.Room;
import entity.RoomBooking;
import entity.RoomBookingHold;
import enums.RoomType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

void main() throws InterruptedException {

    HotelInventory hotel = new HotelInventory("H1", "Grand Sensei Hotel");
    hotel.startHoldExpirationWorker();

    seedRooms(hotel);

    long dayMs = 24L * 60 * 60 * 1000;
    long now = System.currentTimeMillis();
    long checkIn = now + dayMs;
    long checkOut = now + 3 * dayMs;

    printState(hotel, "Initial inventory", checkIn, checkOut);

    section("1. Hold -> Confirm happy path");
    RoomBookingHold hold1 = hotel.holdRoom("hold-1", RoomType.STANDARD, checkIn, checkOut, Duration.ofMinutes(10));
    log("holdRoom(STANDARD) -> %s", describeHold(hold1));
    printState(hotel, "After hold-1", checkIn, checkOut);

    RoomBooking booking1 = hotel.confirmHold("hold-1", "booking-1");
    log("confirmHold(hold-1) -> %s", describeBooking(booking1));
    printState(hotel, "After confirming hold-1", checkIn, checkOut);

    section("2. Cancel hold releases the room immediately");
    RoomBookingHold hold2 = hotel.holdRoom("hold-2", RoomType.STANDARD, checkIn, checkOut, Duration.ofMinutes(10));
    log("holdRoom(STANDARD) -> %s", describeHold(hold2));
    boolean cancelled = hotel.cancelHold("hold-2");
    log("cancelHold(hold-2) -> %s", cancelled);
    printState(hotel, "After cancelling hold-2", checkIn, checkOut);

    section("3. Hold expiration is swept by the background worker");
    RoomBookingHold hold3 = hotel.holdRoom("hold-3", RoomType.STANDARD, checkIn, checkOut, Duration.ofMillis(400));
    log("holdRoom(STANDARD, 400ms TTL) -> %s", describeHold(hold3));
    printState(hotel, "Immediately after hold-3", checkIn, checkOut);
    log("Sleeping 900ms so hold-3 expires and the worker sweeps it...");
    Thread.sleep(900);
    printState(hotel, "After hold-3 expiry", checkIn, checkOut);

    section("4. Concurrency: 5 threads race for the remaining STANDARD rooms");
    raceForRooms(hotel, checkIn, checkOut);
    printState(hotel, "After the race", checkIn, checkOut);

    section("5. removeRoom is blocked while a booking still references the room");
    Room bookedRoom = hotel.getRoom(booking1.getRoomId());
    boolean removedWhileBooked = hotel.removeRoom(bookedRoom.getId());
    log("removeRoom(%s) while booked -> %s (expected false)", bookedRoom.getId(), removedWhileBooked);

    hotel.stopHoldExpirationWorker();
    log("Simulation complete.");
}

// ============================================================
// SETUP
// ============================================================

private void seedRooms(HotelInventory hotel) {
    hotel.addRoom(new Room("R-101", "Standard 101", RoomType.STANDARD, new BigDecimal("99.00")));
    hotel.addRoom(new Room("R-102", "Standard 102", RoomType.STANDARD, new BigDecimal("99.00")));
    hotel.addRoom(new Room("R-103", "Standard 103", RoomType.STANDARD, new BigDecimal("99.00")));
    hotel.addRoom(new Room("R-201", "Twin 201", RoomType.TWIN_BED, new BigDecimal("129.00")));
    hotel.addRoom(new Room("R-202", "Twin 202", RoomType.TWIN_BED, new BigDecimal("129.00")));
}

// ============================================================
// CONCURRENCY DEMO
// ============================================================

private void raceForRooms(HotelInventory hotel, long checkIn, long checkOut) throws InterruptedException {

    int racers = 5;

    ExecutorService pool = Executors.newFixedThreadPool(racers);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch doneGate = new CountDownLatch(racers);
    AtomicInteger successes = new AtomicInteger();
    List<String> results = new CopyOnWriteArrayList<>();

    for (int i = 0; i < racers; i++) {
        int racerId = i;
        pool.submit(() -> {
            try {
                startGate.await();
                RoomBookingHold hold = hotel.holdRoom(
                        "race-" + racerId, RoomType.STANDARD, checkIn, checkOut, Duration.ofMinutes(5)
                );
                if (hold != null) {
                    successes.incrementAndGet();
                    results.add("racer-" + racerId + " WON room " + hold.roomId());
                } else {
                    results.add("racer-" + racerId + " lost (no room available)");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneGate.countDown();
            }
        });
    }

    log("Releasing %d racer threads at once for the same STANDARD room/date range...", racers);
    startGate.countDown();
    doneGate.await();
    pool.shutdown();

    results.forEach(result -> log("  %s", result));
    log("Total successful holds: %d (must equal remaining STANDARD rooms, never more)", successes.get());
}

// ============================================================
// PRINTING
// ============================================================

private void printState(HotelInventory hotel, String label, long checkIn, long checkOut) {
    log(
            "[%s] totalUnits=%d, unitOnHold=%d, remainingInWindow=%d, remainingSTANDARD=%d, remainingTWIN_BED=%d",
            label,
            hotel.totalUnits(),
            hotel.unitOnHold(),
            hotel.remainingUnits(checkIn, checkOut),
            hotel.remainingUnits(RoomType.STANDARD, checkIn, checkOut),
            hotel.remainingUnits(RoomType.TWIN_BED, checkIn, checkOut)
    );
}

private String describeHold(RoomBookingHold hold) {
    if (hold == null) {
        return "null (no room available)";
    }
    return "hold{id=%s, room=%s, checkIn=%s, checkOut=%s, expireAt=%s}".formatted(
            hold.holdId(), hold.roomId(), fmt(hold.checkIn()), fmt(hold.checkOut()), fmt(hold.expireAt())
    );
}

private String describeBooking(RoomBooking booking) {
    if (booking == null) {
        return "null (hold missing or already expired)";
    }
    return "booking{id=%s, room=%s, checkIn=%s, checkOut=%s}".formatted(
            booking.getBookingId(), booking.getRoomId(), booking.getCheckIn(), booking.getCheckOut()
    );
}

private String fmt(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d HH:mm:ss.SSS"));
}

private void log(String pattern, Object... args) {
    System.out.printf("[%s] %s%n", Thread.currentThread().getName(), String.format(pattern, args));
}

private void section(String title) {
    System.out.println();
    System.out.println("=".repeat(70));
    System.out.println(title);
    System.out.println("=".repeat(70));
}
