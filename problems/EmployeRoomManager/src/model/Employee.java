package model;

public class Employee {
    private final String id;
    private final String name;
    private final int age;
    private final String email;
    private final BookingDataManager bookingData; // composition: the employee has its own history, injected

    public Employee(String id, String name, int age, String email, BookingDataManager bookingData) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;
        this.bookingData = bookingData;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public BookingDataManager getBookingData() {
        return bookingData;
    }

    @Override
    public String toString() {
        return "Employee{id=" + id + ", name=" + name + ", age=" + age + ", email=" + email + "}";
    }
}
