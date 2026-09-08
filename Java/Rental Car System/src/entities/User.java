package entities;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

public class User {

    private final String name;
    private final Indentity identity;
    private final List<Reservation> reservations;

    public User(String name, Indentity identity) {
        this.name = name;
        this.identity = identity;
        this.reservations = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Indentity getIdentity() {
        return identity;
    }

    public int getUserAge() {
        return Period.between(identity.getUserDob(), LocalDate.now()).getYears();
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
    }

    public void removeReservation(Reservation reservation) {
        reservations.remove(reservation);
    }
}
