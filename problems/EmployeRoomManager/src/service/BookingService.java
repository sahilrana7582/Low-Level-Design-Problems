package service;

import exception.AlreadyCancelledException;
import exception.BookingNotFoundException;
import exception.EmployeeAlreadyBookedException;
import exception.EmployeeNotFoundException;
import exception.NotAuthorizedException;
import exception.RoomAlreadyBookedException;
import exception.RoomCapacityExceededException;
import model.Booking;
import model.BookingDataManager;
import model.BookingState;
import model.Room;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Manages all bookings: the history of every room and its own record of every employee's bookings.
// A booking is stored in three places: room record, employee record (here) and EmployeeService.
public class BookingService {
    private final RoomService roomService;
    private final EmployeeService employeeService;
    private final Map<String, BookingDataManager> roomBookings = new HashMap<>();
    private final Map<String, BookingDataManager> employeeBookings = new HashMap<>();
    private int nextBookingNumber = 1;

    public BookingService(RoomService roomService, EmployeeService employeeService) {
        this.roomService = roomService;
        this.employeeService = employeeService;
    }

    public List<Booking> getRoomBookingData(String roomId, BookingState state) {
        return getRoomData(roomId).getBookings(state);
    }

    public List<Booking> getEmployeeRoomData(String employeeId, BookingState state) {
        return getEmployeeData(employeeId).getBookings(state);
    }

    // The system assigns the id and the state (BOOKED). Returns the new booking.
    public Booking newBooking(String employeeId, String roomId, LocalDateTime start, LocalDateTime end, int headcount) {
        BookingDataManager employeeData = getEmployeeData(employeeId); // EmployeeNotFoundException
        Room room = roomService.getRoom(roomId);                       // RoomNotFoundException
        BookingDataManager roomData = getRoomData(roomId);

        checkTimeRange(start, end);
        if (headcount < 1) {
            throw new IllegalArgumentException("Headcount must be at least 1");
        }
        if (headcount > room.getCapacity()) {
            throw new RoomCapacityExceededException("Room " + roomId + " seats " + room.getCapacity()
                    + " but " + headcount + " people were requested");
        }
        if (roomData.hasOverlap(start, end)) {
            throw new RoomAlreadyBookedException("Room " + roomId + " is already booked between "
                    + start + " and " + end);
        }
        if (employeeData.hasOverlap(start, end)) {
            throw new EmployeeAlreadyBookedException("Employee " + employeeId + " already has a booking between "
                    + start + " and " + end);
        }

        Booking booking = new Booking("B" + nextBookingNumber++, employeeId, roomId,
                BookingState.BOOKED, start, end, headcount);

        try {
            roomData.addBooking(booking);
            employeeData.addBooking(booking);
            employeeService.addBooking(booking); // last step: if this one fails nothing was stored there
        } catch (RuntimeException e) {
            roomData.removeBooking(booking.getId(), BookingState.BOOKED);
            employeeData.removeBooking(booking.getId(), BookingState.BOOKED);
            throw e;
        }
        return booking;
    }

    // True when no BOOKED booking of the room overlaps the requested range. Back-to-back is fine.
    public boolean checkAvailableRoom(String roomId, LocalDateTime start, LocalDateTime end) {
        checkTimeRange(start, end);
        return !getRoomData(roomId).hasOverlap(start, end);
    }

    // Same, and the room must also seat at least `capacity` people
    public boolean checkAvailableRoom(String roomId, LocalDateTime start, LocalDateTime end, int capacity) {
        Room room = roomService.getRoom(roomId);
        return room.getCapacity() >= capacity && checkAvailableRoom(roomId, start, end);
    }

    // For an employee who has a headcount and a slot but no room in mind
    public List<Room> getAvailableRooms(LocalDateTime start, LocalDateTime end, int capacity) {
        List<Room> availableRooms = new ArrayList<>();
        for (Room room : roomService.getAllRooms()) {
            if (checkAvailableRoom(room.getId(), start, end, capacity)) {
                availableRooms.add(room);
            }
        }
        return availableRooms;
    }

    // Only the id and roomId of the given booking are used. The real state is the one stored here,
    // so a stale copy of the booking (still saying BOOKED) gives the right answer.
    public void cancelBooking(String employeeId, Booking booking) {
        Booking stored = findOwnedBooking(employeeId, booking);
        if (stored.getState() == BookingState.CANCELLED) {
            throw new AlreadyCancelledException(stored.getId());
        }
        if (stored.getState() == BookingState.COMPLETED) {
            throw new BookingNotFoundException("Booking " + stored.getId()
                    + " is completed, only BOOKED bookings can be cancelled");
        }
        changeState(stored, BookingState.CANCELLED);
    }

    // Nothing moves a booking to COMPLETED on its own (no clock in the current scope), the caller does it
    public void completeBooking(String employeeId, Booking booking) {
        Booking stored = findOwnedBooking(employeeId, booking);
        if (stored.getState() != BookingState.BOOKED) {
            throw new BookingNotFoundException("Booking " + stored.getId() + " is " + stored.getState()
                    + ", only BOOKED bookings can be completed");
        }
        changeState(stored, BookingState.COMPLETED);
    }

    // Checks that the employee and room exist, the booking exists, and it belongs to the employee
    private Booking findOwnedBooking(String employeeId, Booking booking) {
        getEmployeeData(employeeId);
        Booking stored = getRoomData(booking.getRoomId()).findBooking(booking.getId());
        if (stored == null) {
            throw new BookingNotFoundException("Booking " + booking.getId() + " not found in room " + booking.getRoomId());
        }
        if (!stored.getEmployeeId().equals(employeeId)) {
            throw new NotAuthorizedException("Employee " + employeeId + " is not the owner of booking " + stored.getId());
        }
        return stored;
    }

    // Moves the booking to the new state in all three places. If something fails, everything goes back.
    private void changeState(Booking original, BookingState newState) {
        Booking updated = original.withState(newState);
        BookingDataManager roomData = getRoomData(original.getRoomId());
        BookingDataManager employeeData = getEmployeeData(original.getEmployeeId());

        try {
            roomData.addBooking(updated);
            employeeData.addBooking(updated);
            employeeService.addBooking(updated); // last step: if this one fails nothing was changed there
        } catch (RuntimeException e) {
            roomData.addBooking(original);
            employeeData.addBooking(original);
            throw e;
        }
    }

    private void checkTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new IllegalArgumentException("A booking must start and end on the same day");
        }
    }

    // Throws RoomNotFoundException for an unknown room. The record is created the first time a room is used.
    private BookingDataManager getRoomData(String roomId) {
        roomService.getRoom(roomId);
        return roomBookings.computeIfAbsent(roomId, id -> new BookingDataManager());
    }

    // Throws EmployeeNotFoundException for an unknown employee. The record is created the first time it is used.
    private BookingDataManager getEmployeeData(String employeeId) {
        if (!employeeService.employeeExist(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
        return employeeBookings.computeIfAbsent(employeeId, id -> new BookingDataManager());
    }
}
