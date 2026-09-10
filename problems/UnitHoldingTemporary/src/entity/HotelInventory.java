package entity;

import enums.RoomType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class HotelInventory implements Inventory {

    private final String id;
    private final String name;

    // ============================================================
    // SOURCE OF TRUTH
    // ============================================================

    private final Map<String, Room> roomsById;

    private final Map<RoomType, Set<String>> roomIdsByType;

    // ============================================================
    // BOOKING STATE
    // ============================================================

    private final Map<String, TreeSet<RoomBooking>> bookingsByRoomId;

    private final Map<String, TreeSet<RoomBookingHold>> holdsByRoomId;

    // ============================================================
    // HOLD EXPIRATION
    // ============================================================

    private final PriorityQueue<RoomBookingHold> pendingHolds;

    // ============================================================
    // CONCURRENCY
    // ============================================================

    private final ReentrantLock lock;

    private volatile boolean running;

    private Thread holdExpirationWorker;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public HotelInventory(
            String id,
            String name
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);

        this.roomsById = new HashMap<>();
        this.roomIdsByType = new HashMap<>();

        this.bookingsByRoomId = new HashMap<>();
        this.holdsByRoomId = new HashMap<>();

        this.pendingHolds = new PriorityQueue<>(
                Comparator.comparingLong(RoomBookingHold::expireAt)
        );

        this.lock = new ReentrantLock();

        this.running = false;
    }

    // ============================================================
    // ROOM MANAGEMENT
    // ============================================================

    public void addRoom(Room room) {

        Objects.requireNonNull(room, "Room is not provided");

        lock.lock();

        try {

            if (roomsById.containsKey(room.getId())) {
                throw new IllegalArgumentException(
                        "Room already exists: " + room.getId()
                );
            }

            roomsById.put(
                    room.getId(),
                    room
            );

            roomIdsByType
                    .computeIfAbsent(
                            room.getType(),
                            key -> new HashSet<>()
                    )
                    .add(room.getId());

            bookingsByRoomId.put(
                    room.getId(),
                    new TreeSet<>(
                            Comparator
                                    .comparing(RoomBooking::getCheckIn)
                                    .thenComparing(RoomBooking::getCheckOut)
                                    .thenComparing(RoomBooking::getBookingId)
                    )
            );

            holdsByRoomId.put(
                    room.getId(),
                    new TreeSet<>(
                            Comparator
                                    .comparing(RoomBookingHold::checkIn)
                                    .thenComparing(RoomBookingHold::checkOut)
                                    .thenComparing(RoomBookingHold::holdId)
                    )
            );

        } finally {
            lock.unlock();
        }
    }

    public boolean removeRoom(String roomId) {

        Objects.requireNonNull(roomId, "Room ID is not provided");

        lock.lock();

        try {

            Room room = roomsById.get(roomId);

            if (room == null) {
                return false;
            }

            TreeSet<RoomBooking> bookings =
                    bookingsByRoomId.get(roomId);

            TreeSet<RoomBookingHold> holds =
                    holdsByRoomId.get(roomId);

            if (bookings != null && !bookings.isEmpty()) {
                return false;
            }

            if (holds != null && !holds.isEmpty()) {
                return false;
            }

            roomsById.remove(roomId);

            Set<String> roomIds =
                    roomIdsByType.get(room.getType());

            if (roomIds != null) {
                roomIds.remove(roomId);

                if (roomIds.isEmpty()) {
                    roomIdsByType.remove(room.getType());
                }
            }

            bookingsByRoomId.remove(roomId);
            holdsByRoomId.remove(roomId);

            return true;

        } finally {
            lock.unlock();
        }
    }

    public Room getRoom(String roomId) {

        Objects.requireNonNull(roomId, "Room ID is not provided");

        lock.lock();

        try {
            return roomsById.get(roomId);
        } finally {
            lock.unlock();
        }
    }

    public Set<String> getRoomIdsByType(RoomType roomType) {

        Objects.requireNonNull(roomType, "Room type is not provided");

        lock.lock();

        try {

            Set<String> roomIds =
                    roomIdsByType.get(roomType);

            if (roomIds == null) {
                return Collections.emptySet();
            }

            return Set.copyOf(roomIds);

        } finally {
            lock.unlock();
        }
    }

    // ============================================================
    // INVENTORY COUNT
    // ============================================================

    @Override
    public int totalUnits() {

        lock.lock();

        try {
            return roomsById.size();
        } finally {
            lock.unlock();
        }
    }

    public int unitOnHold() {

        lock.lock();

        try {

            removeExpiredHoldsInternal();

            int count = 0;

            for (TreeSet<RoomBookingHold> holds
                    : holdsByRoomId.values()) {

                if (!holds.isEmpty()) {
                    count++;
                }
            }

            return count;

        } finally {
            lock.unlock();
        }
    }

    public int remainingUnits() {

        long now = System.currentTimeMillis();

        return remainingUnits(
                now,
                now + 1
        );
    }

    public int remainingUnits(
            long checkIn,
            long checkOut
    ) {

        validateInterval(checkIn, checkOut);

        lock.lock();

        try {

            removeExpiredHoldsInternal();

            int availableRooms = 0;

            for (String roomId : roomsById.keySet()) {

                if (isRoomAvailableInternal(
                        roomId,
                        checkIn,
                        checkOut
                )) {
                    availableRooms++;
                }
            }

            return availableRooms;

        } finally {
            lock.unlock();
        }
    }

    public int remainingUnits(
            RoomType roomType,
            long checkIn,
            long checkOut
    ) {

        Objects.requireNonNull(roomType, "Room type is not provided");

        validateInterval(checkIn, checkOut);

        lock.lock();

        try {

            removeExpiredHoldsInternal();

            Set<String> roomIds =
                    roomIdsByType.get(roomType);

            if (roomIds == null) {
                return 0;
            }

            int availableRooms = 0;

            for (String roomId : roomIds) {

                if (isRoomAvailableInternal(
                        roomId,
                        checkIn,
                        checkOut
                )) {
                    availableRooms++;
                }
            }

            return availableRooms;

        } finally {
            lock.unlock();
        }
    }

    // ============================================================
    // AVAILABILITY
    // ============================================================

    public boolean isRoomAvailable(
            String roomId,
            long checkIn,
            long checkOut
    ) {

        Objects.requireNonNull(roomId, "Room ID is not provided");

        validateInterval(checkIn, checkOut);

        lock.lock();

        try {

            removeExpiredHoldsInternal();

            return isRoomAvailableInternal(
                    roomId,
                    checkIn,
                    checkOut
            );

        } finally {
            lock.unlock();
        }
    }

    private boolean isRoomAvailableInternal(
            String roomId,
            long checkIn,
            long checkOut
    ) {

        if (!roomsById.containsKey(roomId)) {
            return false;
        }

        TreeSet<RoomBooking> bookings =
                bookingsByRoomId.get(roomId);

        if (bookings != null) {

            for (RoomBooking booking : bookings) {

                if (overlaps(
                        booking.getCheckIn().toEpochMilli(),
                        booking.getCheckOut().toEpochMilli(),
                        checkIn,
                        checkOut
                )) {
                    return false;
                }
            }
        }

        TreeSet<RoomBookingHold> holds =
                holdsByRoomId.get(roomId);

        if (holds != null) {

            for (RoomBookingHold hold : holds) {

                if (overlaps(
                        hold.checkIn(),
                        hold.checkOut(),
                        checkIn,
                        checkOut
                )) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean overlaps(
            long existingStart,
            long existingEnd,
            long requestedStart,
            long requestedEnd
    ) {

        return existingStart < requestedEnd
                && existingEnd > requestedStart;
    }

    // ============================================================
    // FIND AVAILABLE ROOM
    // ============================================================

    public Room findAvailableRoom(
            RoomType roomType,
            long checkIn,
            long checkOut
    ) {

        Objects.requireNonNull(roomType, "Room type is not provided");

        validateInterval(checkIn, checkOut);

        lock.lock();

        try {

            removeExpiredHoldsInternal();

            Set<String> roomIds =
                    roomIdsByType.get(roomType);

            if (roomIds == null) {
                return null;
            }

            for (String roomId : roomIds) {

                if (isRoomAvailableInternal(
                        roomId,
                        checkIn,
                        checkOut
                )) {
                    return roomsById.get(roomId);
                }
            }

            return null;

        } finally {
            lock.unlock();
        }
    }

    // ============================================================
    // CREATE HOLD
    // ============================================================

    public RoomBookingHold holdRoom(
            String holdId,
            RoomType roomType,
            long checkIn,
            long checkOut,
            Duration holdDuration
    ) {

        Objects.requireNonNull(holdId, "Hold ID is not provided");
        Objects.requireNonNull(roomType, "Room type is not provided");
        Objects.requireNonNull(holdDuration, "Hold duration is not provided");

        if (holdDuration.isZero() || holdDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "Hold duration must be positive"
            );
        }

        validateInterval(checkIn, checkOut);

        lock.lock();

        try {

            removeExpiredHoldsInternal();

            Set<String> roomIds =
                    roomIdsByType.get(roomType);

            if (roomIds == null) {
                return null;
            }

            String availableRoomId = null;

            for (String roomId : roomIds) {

                if (isRoomAvailableInternal(
                        roomId,
                        checkIn,
                        checkOut
                )) {
                    availableRoomId = roomId;
                    break;
                }
            }

            if (availableRoomId == null) {
                return null;
            }

            long expireAt =
                    System.currentTimeMillis()
                            + holdDuration.toMillis();

            RoomBookingHold hold =
                    new RoomBookingHold(
                            holdId,
                            availableRoomId,
                            checkIn,
                            checkOut,
                            expireAt
                    );

            holdsByRoomId
                    .get(availableRoomId)
                    .add(hold);

            pendingHolds.add(hold);

            return hold;

        } finally {
            lock.unlock();
        }
    }

    // ============================================================
    // CONFIRM HOLD -> BOOKING
    // ============================================================

    public RoomBooking confirmHold(
            String holdId,
            String bookingId
    ) {

        Objects.requireNonNull(holdId, "Hold ID is not provided");
        Objects.requireNonNull(bookingId, "Booking ID is not provided");

        lock.lock();

        try {

            removeExpiredHoldsInternal();

            RoomBookingHold hold =
                    findHoldInternal(holdId);

            if (hold == null) {
                return null;
            }

            TreeSet<RoomBookingHold> holds =
                    holdsByRoomId.get(hold.roomId());

            holds.remove(hold);

            pendingHolds.remove(hold);

            RoomBooking booking =
                    new RoomBooking(
                            bookingId,
                            hold.roomId(),
                            Instant.ofEpochMilli(hold.checkIn()),
                            Instant.ofEpochMilli(hold.checkOut())
                    );

            bookingsByRoomId
                    .get(hold.roomId())
                    .add(booking);

            return booking;

        } finally {
            lock.unlock();
        }
    }

    // ============================================================
    // CANCEL HOLD
    // ============================================================

    public boolean cancelHold(String holdId) {

        Objects.requireNonNull(holdId, "Hold ID is not provided");

        lock.lock();

        try {

            RoomBookingHold hold =
                    findHoldInternal(holdId);

            if (hold == null) {
                return false;
            }

            TreeSet<RoomBookingHold> holds =
                    holdsByRoomId.get(hold.roomId());

            holds.remove(hold);

            pendingHolds.remove(hold);

            return true;

        } finally {
            lock.unlock();
        }
    }

    // ============================================================
    // FIND HOLD
    // ============================================================

    private RoomBookingHold findHoldInternal(
            String holdId
    ) {

        for (TreeSet<RoomBookingHold> holds
                : holdsByRoomId.values()) {

            for (RoomBookingHold hold : holds) {

                if (hold.holdId().equals(holdId)) {
                    return hold;
                }
            }
        }

        return null;
    }

    // ============================================================
    // EXPIRE HOLDS
    // ============================================================

    private void removeExpiredHoldsInternal() {

        long now = System.currentTimeMillis();

        while (!pendingHolds.isEmpty()) {

            RoomBookingHold hold =
                    pendingHolds.peek();

            if (hold.expireAt() > now) {
                break;
            }

            pendingHolds.poll();

            TreeSet<RoomBookingHold> holds =
                    holdsByRoomId.get(hold.roomId());

            if (holds != null) {
                holds.remove(hold);
            }
        }
    }

    // ============================================================
    // BACKGROUND HOLD EXPIRATION WORKER
    // ============================================================

    public void startHoldExpirationWorker() {

        lock.lock();

        try {

            if (running) {
                return;
            }

            running = true;

            holdExpirationWorker =
                    new Thread(
                            this::runHoldExpirationWorker,
                            "hotel-hold-expiration-worker"
                    );

            holdExpirationWorker.setDaemon(true);

            holdExpirationWorker.start();

        } finally {
            lock.unlock();
        }
    }

    private void runHoldExpirationWorker() {

        while (running) {

            long waitTime;

            lock.lock();

            try {

                removeExpiredHoldsInternal();

                RoomBookingHold nextHold =
                        pendingHolds.peek();

                if (nextHold == null) {
                    waitTime = 1000;
                } else {

                    long now =
                            System.currentTimeMillis();

                    waitTime = Math.min(
                            nextHold.expireAt() - now,
                            1000
                    );
                }

            } finally {
                lock.unlock();
            }

            if (waitTime > 0) {
                sleep(waitTime);
            }
        }
    }

    public void stopHoldExpirationWorker() {

        lock.lock();

        try {

            running = false;

            if (holdExpirationWorker != null) {
                holdExpirationWorker.interrupt();
                holdExpirationWorker = null;
            }

        } finally {
            lock.unlock();
        }
    }

    private void sleep(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    private void validateInterval(
            long checkIn,
            long checkOut
    ) {

        if (checkIn >= checkOut) {
            throw new IllegalArgumentException(
                    "checkIn must be before checkOut"
            );
        }
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}