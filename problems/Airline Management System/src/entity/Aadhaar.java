package entity;

import java.time.LocalDate;

public class Aadhaar extends Identity{
    private String aadhaarNumber;

    public Aadhaar(String fullName, LocalDate dateOfBirth, Location address, String aadhaarNumber) {
        super(fullName, dateOfBirth, address);
        this.aadhaarNumber = aadhaarNumber;
    }

    @Override
    public String getIdentityNumber() {
        return this.aadhaarNumber;
    }
}
