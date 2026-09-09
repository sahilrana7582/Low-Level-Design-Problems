package entity;

import enums.PassengerType;

public abstract class Passenger {
    private String firstName;
    private String lastName;
    private String email;
    private String phNumber;
    private Identity identityProof;
    private PassengerType passengerType;

    protected Passenger(String firstName, String lastName, String email, String phNumber,
                         Identity identityProof, PassengerType passengerType) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phNumber = phNumber;
        this.identityProof = identityProof;
        this.passengerType = passengerType;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhNumber() {
        return phNumber;
    }

    public Identity getIdentityProof() {
        return identityProof;
    }

    public PassengerType getPassengerType() {
        return passengerType;
    }

    public abstract String getPassengerInfo();
}
