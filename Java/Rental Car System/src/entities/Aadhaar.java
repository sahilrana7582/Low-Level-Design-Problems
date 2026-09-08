package entities;

import java.time.LocalDate;

public class Aadhaar extends Indentity {

    private final String aadhaarNumber;

    public Aadhaar(String userName, LocalDate userDob, String aadhaarNumber) {
        super(userName, userDob);

        if (aadhaarNumber == null || !aadhaarNumber.matches("\\d{12}")) {
            throw new IllegalArgumentException(
                    "Aadhaar number must be exactly 12 digits"
            );
        }

        this.aadhaarNumber = aadhaarNumber;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    @Override
    public String getIdentityNumber() {
        return aadhaarNumber;
    }
}
