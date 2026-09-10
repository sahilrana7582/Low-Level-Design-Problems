package entity;

import java.time.LocalDate;

public class Passport extends Identity{
    private String passportNumber;

    public Passport(String fullName, LocalDate dateOfBirth, Location address, String passportNumber) {
        super(fullName, dateOfBirth, address);
        this.passportNumber = passportNumber;
    }

    @Override
    public String getIdentityNumber() {
        return this.passportNumber;
    }
}
