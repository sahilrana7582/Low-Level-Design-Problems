import exception.AlreadyCancelledException;
import exception.BookingNotFoundException;
import exception.EmployeeAlreadyBookedException;
import exception.EmployeeNotFoundException;
import exception.NotAuthorizedException;
import exception.RoomAlreadyBookedException;
import exception.RoomCapacityExceededException;
import exception.RoomNotFoundException;
import model.Booking;
import model.BookingDataManager;
import model.BookingState;
import model.Employee;
import model.Room;
import service.BookingService;
import service.EmployeeService;
import service.RoomService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Main {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        RoomService roomService = new RoomService();
        EmployeeService employeeService = new EmployeeService();
        BookingService bookingService = new BookingService(roomService, employeeService);

        // ---------------------------------------------------------------
        section("Setup");
        roomService.addRoom(new Room("R1", "Alpha", 1, 6));
        roomService.addRoom(new Room("R2", "Beta", 2, 12));
        roomService.addRoom(new Room("R3", "Gamma", 2, 12));
        roomService.addRoom(new Room("R4", "Delta", 3, 20));
        // every employee gets its own new BookingDataManager (injected)
        employeeService.addEmployee(new Employee("E1", "Alice", 28, "alice@company.com", new BookingDataManager()));
        employeeService.addEmployee(new Employee("E2", "Bob", 34, "bob@company.com", new BookingDataManager()));
        employeeService.addEmployee(new Employee("E3", "Carol", 41, "carol@company.com", new BookingDataManager()));
        expect("new employee with zero bookings exists", true, employeeService.employeeExist("E1"));
        expect("unknown employee does not exist", false, employeeService.employeeExist("E9"));
        expect("rooms added", Arrays.asList("R1", "R2", "R3", "R4"), roomIds(roomService.getAllRooms()));

        // ---------------------------------------------------------------
        section("Happy path: E1 books R1 10-11, the system assigns the id");
        Booking b1 = bookingService.newBooking("E1", "R1", at(10), at(11), 4);
        System.out.println("  returned: " + b1);
        expect("id assigned", true, b1.getId() != null && !b1.getId().isEmpty());
        expect("state", BookingState.BOOKED, b1.getState());
        expect("R1 booked", Collections.singletonList(b1.getId()), ids(bookingService.getRoomBookingData("R1", BookingState.BOOKED)));
        expect("E1 booked (BookingService)", Collections.singletonList(b1.getId()), ids(bookingService.getEmployeeRoomData("E1", BookingState.BOOKED)));
        expect("E1 booked (EmployeeService)", Collections.singletonList(b1.getId()), ids(employeeService.getBooking("E1")));

        // ---------------------------------------------------------------
        section("Back-to-back: E2 books R1 11-12");
        Booking b2 = bookingService.newBooking("E2", "R1", at(11), at(12), 3);
        expect("R1 booked", Arrays.asList(b1.getId(), b2.getId()), ids(bookingService.getRoomBookingData("R1", BookingState.BOOKED)));
        expect("R1 free 09-10 (ends when B1 starts)", true, bookingService.checkAvailableRoom("R1", at(9), at(10)));
        expect("R1 free 12-13 (starts when B2 ends)", true, bookingService.checkAvailableRoom("R1", at(12), at(13)));

        // ---------------------------------------------------------------
        section("Overlaps on R1 (B1 10-11, B2 11-12), requested by E3");
        expectError("inside B1 (10:15-10:45)", RoomAlreadyBookedException.class,
                () -> bookingService.newBooking("E3", "R1", at(10, 15), at(10, 45), 2));
        expectError("wrapping both (09:00-13:00)", RoomAlreadyBookedException.class,
                () -> bookingService.newBooking("E3", "R1", at(9), at(13), 2));
        expectError("partial, starts before B1 (09:30-10:30)", RoomAlreadyBookedException.class,
                () -> bookingService.newBooking("E3", "R1", at(9, 30), at(10, 30), 2));
        expectError("partial, ends after B2 (11:30-12:30)", RoomAlreadyBookedException.class,
                () -> bookingService.newBooking("E3", "R1", at(11, 30), at(12, 30), 2));
        expectError("exact same slot (10:00-11:00)", RoomAlreadyBookedException.class,
                () -> bookingService.newBooking("E3", "R1", at(10), at(11), 2));
        expect("R1 still has only B1, B2", Arrays.asList(b1.getId(), b2.getId()), ids(bookingService.getRoomBookingData("R1", BookingState.BOOKED)));
        expect("E3 has nothing", Collections.emptyList(), ids(employeeService.getBooking("E3")));

        // ---------------------------------------------------------------
        section("Employee conflict: E1 has R1 10-11, then asks for R2 10:30-11:30");
        expectError("E1 double booked", EmployeeAlreadyBookedException.class,
                () -> bookingService.newBooking("E1", "R2", at(10, 30), at(11, 30), 4));
        expect("R2 stays empty", Collections.emptyList(), ids(bookingService.getRoomBookingData("R2", BookingState.BOOKED)));
        expect("E1 still has only B1", Collections.singletonList(b1.getId()), ids(employeeService.getBooking("E1")));

        // ---------------------------------------------------------------
        section("Headcount");
        expectError("12 people in the 6-seat R1", RoomCapacityExceededException.class,
                () -> bookingService.newBooking("E3", "R1", at(14), at(15), 12));
        expectError("0 people", IllegalArgumentException.class,
                () -> bookingService.newBooking("E3", "R2", at(14), at(15), 0));
        Booking b3 = bookingService.newBooking("E3", "R3", at(10), at(12), 12); // exactly the capacity is fine
        expect("12 people in the 12-seat R3", BookingState.BOOKED, b3.getState());

        // ---------------------------------------------------------------
        section("Unknown ids");
        expectError("unknown employee E9", EmployeeNotFoundException.class,
                () -> bookingService.newBooking("E9", "R1", at(14), at(15), 2));
        expectError("unknown room R9", RoomNotFoundException.class,
                () -> bookingService.newBooking("E1", "R9", at(14), at(15), 2));
        expectError("availability of unknown room R9", RoomNotFoundException.class,
                () -> bookingService.checkAvailableRoom("R9", at(14), at(15)));
        expectError("room history of unknown room R9", RoomNotFoundException.class,
                () -> bookingService.getRoomBookingData("R9", BookingState.BOOKED));
        expectError("employee history of unknown employee E9", EmployeeNotFoundException.class,
                () -> bookingService.getEmployeeRoomData("E9", BookingState.BOOKED));
        expectError("EmployeeService history of unknown employee E9", EmployeeNotFoundException.class,
                () -> employeeService.getBooking("E9"));

        // ---------------------------------------------------------------
        section("Bad time");
        expectError("end equals start", IllegalArgumentException.class,
                () -> bookingService.newBooking("E3", "R2", at(14), at(14), 2));
        expectError("end before start", IllegalArgumentException.class,
                () -> bookingService.newBooking("E3", "R2", at(15), at(14), 2));
        expectError("different days", IllegalArgumentException.class,
                () -> bookingService.newBooking("E3", "R2", at(23), at(1).plusDays(1), 2));
        expectError("availability with end before start", IllegalArgumentException.class,
                () -> bookingService.checkAvailableRoom("R2", at(15), at(14)));

        // ---------------------------------------------------------------
        section("Cancel");
        expectError("non-owner E2 cancels B1", NotAuthorizedException.class,
                () -> bookingService.cancelBooking("E2", b1));
        Booking unknown = new Booking("B999", "E1", "R1", BookingState.BOOKED, at(10), at(11), 1);
        expectError("unknown booking id B999", BookingNotFoundException.class,
                () -> bookingService.cancelBooking("E1", unknown));

        bookingService.cancelBooking("E1", b1); // the owner cancels
        expect("R1 booked after cancel", Collections.singletonList(b2.getId()), ids(bookingService.getRoomBookingData("R1", BookingState.BOOKED)));
        expect("R1 cancelled after cancel", Collections.singletonList(b1.getId()), ids(bookingService.getRoomBookingData("R1", BookingState.CANCELLED)));
        expect("R1 10-11 is free again", true, bookingService.checkAvailableRoom("R1", at(10), at(11)));

        Booking b4 = bookingService.newBooking("E2", "R1", at(10), at(11), 3); // E2 takes the freed slot
        expect("E2 booked the freed slot", BookingState.BOOKED, b4.getState());

        // b1 is the caller's old copy (it still says BOOKED), the system answers from its own stored state
        expectError("cancel B1 a second time", AlreadyCancelledException.class,
                () -> bookingService.cancelBooking("E1", b1));

        // ---------------------------------------------------------------
        section("Histories");
        expect("R1 cancelled history", Collections.singletonList(b1.getId()), ids(bookingService.getRoomBookingData("R1", BookingState.CANCELLED)));
        expect("E1 cancelled history (BookingService)", Collections.singletonList(b1.getId()), ids(bookingService.getEmployeeRoomData("E1", BookingState.CANCELLED)));
        expect("E1 cancelled history (EmployeeService)", Collections.singletonList(b1.getId()), ids(employeeService.getCancelled("E1")));
        expect("E1 current list is active only (empty)", Collections.emptyList(), ids(employeeService.getBooking("E1")));
        expect("E2 current list in start order (B4 10:00 before B2 11:00)", Arrays.asList(b4.getId(), b2.getId()), ids(employeeService.getBooking("E2")));
        expect("R1 booked list in start order", Arrays.asList(b4.getId(), b2.getId()), ids(bookingService.getRoomBookingData("R1", BookingState.BOOKED)));

        List<Booking> returned = employeeService.getBooking("E2");
        returned.clear();
        expect("clearing a returned employee list changes nothing", 2, employeeService.getBooking("E2").size());
        List<Booking> returnedRoomList = bookingService.getRoomBookingData("R1", BookingState.BOOKED);
        returnedRoomList.clear();
        expect("clearing a returned room list changes nothing", 2, bookingService.getRoomBookingData("R1", BookingState.BOOKED).size());

        // ---------------------------------------------------------------
        section("Free rooms (R1 has 6 seats, R2 12, R3 12, R4 20; R1 and R3 are busy 10-12)");
        expect("12 people, 10:30-11:00 (excludes 6-seat R1 and busy R3)", Arrays.asList("R2", "R4"),
                roomIds(bookingService.getAvailableRooms(at(10, 30), at(11), 12)));
        expect("12 people, 13:00-14:00 (only the 6-seat R1 is excluded)", Arrays.asList("R2", "R3", "R4"),
                roomIds(bookingService.getAvailableRooms(at(13), at(14), 12)));
        expect("5 people, 13:00-14:00 (all free)", Arrays.asList("R1", "R2", "R3", "R4"),
                roomIds(bookingService.getAvailableRooms(at(13), at(14), 5)));
        expect("R1 with capacity 12 at 13:00 (too small)", false, bookingService.checkAvailableRoom("R1", at(13), at(14), 12));
        expect("R2 with capacity 12 at 13:00", true, bookingService.checkAvailableRoom("R2", at(13), at(14), 12));

        // ---------------------------------------------------------------
        section("Complete (extra)");
        expectError("non-owner E1 completes B2", NotAuthorizedException.class,
                () -> bookingService.completeBooking("E1", b2));
        bookingService.completeBooking("E2", b2);
        expect("R1 completed", Collections.singletonList(b2.getId()), ids(bookingService.getRoomBookingData("R1", BookingState.COMPLETED)));
        expect("E2 completed (EmployeeService)", Collections.singletonList(b2.getId()), ids(employeeService.getCompleted("E2")));
        expect("E2 current list after complete", Collections.singletonList(b4.getId()), ids(employeeService.getBooking("E2")));
        expectError("cancel a completed booking", BookingNotFoundException.class,
                () -> bookingService.cancelBooking("E2", b2));
        expectError("complete a cancelled booking", BookingNotFoundException.class,
                () -> bookingService.completeBooking("E1", b1));

        // ---------------------------------------------------------------
        section("Rollback (extra): EmployeeService fails in the last step");
        rollbackScenario();

        // ---------------------------------------------------------------
        System.out.println();
        System.out.println("=== Result: " + passed + " passed, " + failed + " failed ===");
    }

    // EmployeeService that can be told to fail, to prove that BookingService puts everything back
    private static class FailingEmployeeService extends EmployeeService {
        boolean fail = false;

        @Override
        public void addBooking(Booking booking) {
            if (fail) {
                throw new IllegalStateException("simulated failure in EmployeeService");
            }
            super.addBooking(booking);
        }
    }

    private static void rollbackScenario() {
        RoomService roomService = new RoomService();
        FailingEmployeeService employeeService = new FailingEmployeeService();
        BookingService bookingService = new BookingService(roomService, employeeService);
        roomService.addRoom(new Room("R1", "Alpha", 1, 6));
        employeeService.addEmployee(new Employee("E1", "Alice", 28, "alice@company.com", new BookingDataManager()));

        employeeService.fail = true;
        expectError("new booking while EmployeeService fails", IllegalStateException.class,
                () -> bookingService.newBooking("E1", "R1", at(10), at(11), 2));
        expect("room record is empty again", Collections.emptyList(), ids(bookingService.getRoomBookingData("R1", BookingState.BOOKED)));
        expect("employee record (BookingService) is empty again", Collections.emptyList(), ids(bookingService.getEmployeeRoomData("E1", BookingState.BOOKED)));
        expect("R1 10-11 is still free", true, bookingService.checkAvailableRoom("R1", at(10), at(11)));

        employeeService.fail = false;
        Booking booking = bookingService.newBooking("E1", "R1", at(10), at(11), 2);

        employeeService.fail = true;
        expectError("cancel while EmployeeService fails", IllegalStateException.class,
                () -> bookingService.cancelBooking("E1", booking));
        expect("room record still BOOKED", Collections.singletonList(booking.getId()), ids(bookingService.getRoomBookingData("R1", BookingState.BOOKED)));
        expect("room record has nothing CANCELLED", Collections.emptyList(), ids(bookingService.getRoomBookingData("R1", BookingState.CANCELLED)));
        expect("employee record (BookingService) still BOOKED", Collections.singletonList(booking.getId()), ids(bookingService.getEmployeeRoomData("E1", BookingState.BOOKED)));
        expect("employee record (BookingService) nothing CANCELLED", Collections.emptyList(), ids(bookingService.getEmployeeRoomData("E1", BookingState.CANCELLED)));
        expect("employee record (EmployeeService) still BOOKED", Collections.singletonList(booking.getId()), ids(employeeService.getBooking("E1")));
    }

    // ----- small helpers -----

    private static LocalDateTime at(int hour) {
        return at(hour, 0);
    }

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 10, 1, hour, minute);
    }

    private static List<String> ids(List<Booking> bookings) {
        List<String> ids = new ArrayList<>();
        for (Booking booking : bookings) {
            ids.add(booking.getId());
        }
        return ids;
    }

    private static List<String> roomIds(List<Room> rooms) {
        List<String> ids = new ArrayList<>();
        for (Room room : rooms) {
            ids.add(room.getId());
        }
        return ids;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void expect(String scenario, Object expected, Object actual) {
        boolean ok = Objects.equals(expected, actual);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println("  " + (ok ? "PASS" : "FAIL") + " | " + scenario
                + " | expected: " + expected + " | actual: " + actual);
    }

    // Runs the action and expects exactly this exception type
    private static void expectError(String scenario, Class<? extends RuntimeException> expected, Runnable action) {
        String actual;
        boolean ok;
        try {
            action.run();
            actual = "no exception";
            ok = false;
        } catch (RuntimeException e) {
            actual = e.getClass().getSimpleName() + " (" + e.getMessage() + ")";
            ok = e.getClass().equals(expected);
        }
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println("  " + (ok ? "PASS" : "FAIL") + " | " + scenario
                + " | expected: " + expected.getSimpleName() + " | actual: " + actual);
    }
}
