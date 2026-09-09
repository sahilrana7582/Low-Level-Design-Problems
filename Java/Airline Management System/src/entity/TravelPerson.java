package entity;

import enums.PassengerType;

public class TravelPerson extends Passenger{

    public TravelPerson(String firstName, String lastName, String email, String phNumber, Identity identityProof) {
        super(firstName, lastName, email, phNumber, identityProof, PassengerType.TRAVEL_PERSON);
    }

    @Override
    public String getPassengerInfo() {
        return "Passenger[name=" + getFirstName() + " " + getLastName()
                + ", email=" + getEmail() + ", phone=" + getPhNumber()
                + ", identity=" + getIdentityProof().getIdentityNumber()
                + ", type=" + getPassengerType() + "]";
    }
}
