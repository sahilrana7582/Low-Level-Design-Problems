package entity;


import enums.SeatStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Airplane {
    private String id;
    private String name;
    private Route route;
    private LocalDateTime takeOffTime;
    private ArrayList<Seat> seatsList;
    private List<AirplaneCrew> airplaneCrewList;
    private List<AirplanePilot> airplanePilots;

    public Airplane(String id, String name, Route route, LocalDateTime takeOffTime, ArrayList<Seat> seatsList,
                     List<AirplaneCrew> airplaneCrewList, List<AirplanePilot> airplanePilots) {
        this.id = id;
        this.name = name;
        this.route = route;
        this.takeOffTime = takeOffTime;
        this.seatsList = seatsList != null ? seatsList : new ArrayList<>();
        this.airplaneCrewList = airplaneCrewList != null ? airplaneCrewList : new ArrayList<>();
        this.airplanePilots = airplanePilots != null ? airplanePilots : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Route getRoute() {
        return route;
    }

    public LocalDateTime getTakeOffTime() {
        return takeOffTime;
    }

    public List<AirplaneCrew> getAirplaneCrewList() {
        return airplaneCrewList;
    }

    public List<AirplanePilot> getAirplanePilots() {
        return airplanePilots;
    }

    public List<Seat> getSeats() {
        System.out.println("[AIRPLANE] Fetching " + seatsList.size() + " seat(s) for flight " + name);
        return seatsList;
    }

    public String bookSeat(Seat seat){
        if (seat == null || !seatsList.contains(seat)) {
            String message = "Seat does not belong to flight " + name;
            System.out.println("[AIRPLANE] " + message);
            return message;
        }

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            String message = "Seat " + seat.getNumber() + " on flight " + name
                    + " is not available (current status: " + seat.getStatus() + ")";
            System.out.println("[AIRPLANE] " + message);
            return message;
        }

        seat.setStatus(SeatStatus.BOOKED);
        String message = "Seat " + seat.getNumber() + " on flight " + name + " successfully booked";
        System.out.println("[AIRPLANE] " + message);
        return message;
    }

    @Override
    public String toString() {
        return "Airplane[id=" + id + ", name=" + name + ", route=" + route + ", takeOff=" + takeOffTime + "]";
    }
}
