package entities;

import java.time.LocalDate;

public class Passport extends Indentity {

    private final String passportNumber;
    private final String issuingCountry;
    private final LocalDate expiryDate;

    public Passport(
            String userName,
            LocalDate userDob,
            String passportNumber,
            String issuingCountry,
            LocalDate expiryDate) {

        super(userName, userDob);

        this.passportNumber = passportNumber;
        this.issuingCountry = issuingCountry;
        this.expiryDate = expiryDate;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public String getIssuingCountry() {
        return issuingCountry;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    @Override
    public String getIdentityNumber() {
        return passportNumber;
    }
}
