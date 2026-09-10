package entity;

import java.time.LocalDate;
import java.time.Period;

public abstract class Identity {
    private String fullName;
    private LocalDate dateOfBirth;
    private Location address;

    protected Identity(String fullName, LocalDate dateOfBirth, Location address) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Location getAddress() {
        return address;
    }

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public abstract String getIdentityNumber();
}
