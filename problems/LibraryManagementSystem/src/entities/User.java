package entities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class User {

    private final String id;
    private final String name;
    private final int age;
    private final String email;

    private BigDecimal debt;
    private final List<BookHistory> bookingHistory;

    public User(
            String id,
            String name,
            int age,
            String email
    ) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;

        this.debt = BigDecimal.ZERO;
        this.bookingHistory = new ArrayList<>();
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

    public BigDecimal getDebt() {
        return debt;
    }

    public List<BookHistory> getBookingHistory() {
        return List.copyOf(bookingHistory);
    }

    public void addDebt(BigDecimal amount) {
        this.debt = this.debt.add(amount);
    }

    public void clearDebt() {
        this.debt = BigDecimal.ZERO;
    }

    public void addBookHistory(BookHistory history) {
        this.bookingHistory.add(history);
    }
}