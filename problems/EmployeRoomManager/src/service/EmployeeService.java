package service;

import exception.BookingNotFoundException;
import exception.EmployeeNotFoundException;
import model.Booking;
import model.BookingDataManager;
import model.BookingState;
import model.Employee;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Owns the Employee objects and each employee's own booking history.
// This history is separate from BookingService's copy, so it survives if BookingService ever drops its data.
public class EmployeeService {
    private final Map<String, Employee> employees = new HashMap<>();

    public void addEmployee(Employee employee) {
        if (employeeExist(employee.getId())) {
            throw new IllegalArgumentException("Employee already exists: " + employee.getId());
        }
        employees.put(employee.getId(), employee);
    }

    // True for every added employee, even one with zero bookings
    public boolean employeeExist(String employeeId) {
        return employees.containsKey(employeeId);
    }

    public Employee getEmployee(String employeeId) {
        Employee employee = employees.get(employeeId);
        if (employee == null) {
            throw new EmployeeNotFoundException(employeeId);
        }
        return employee;
    }

    public List<Booking> getCancelled(String employeeId) {
        return getBookingData(employeeId).getCancelledBookings();
    }

    // The employee's current bookings: BOOKED only, in start order
    public List<Booking> getBooking(String employeeId) {
        return getBookingData(employeeId).getBookedBookings();
    }

    public List<Booking> getCompleted(String employeeId) {
        return getBookingData(employeeId).getCompletedBookings();
    }

    // Files the booking into the list of its state (moves it if the id is already stored in another state)
    public void addBooking(Booking booking) {
        getBookingData(booking.getEmployeeId()).addBooking(booking);
    }

    // Not used in the current scope (nothing is deleted). Kept for the future.
    public void removeBooking(String employeeId, String bookingId, BookingState state) {
        if (!getBookingData(employeeId).removeBooking(bookingId, state)) {
            throw new BookingNotFoundException("Booking " + bookingId + " not found in "
                    + state + " bookings of employee " + employeeId);
        }
    }

    private BookingDataManager getBookingData(String employeeId) {
        return getEmployee(employeeId).getBookingData();
    }
}
