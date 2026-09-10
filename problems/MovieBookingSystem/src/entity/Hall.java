package entity;

import java.util.ArrayList;
import java.util.List;

public class Hall {

    private final String id;
    private final String name;
    private final CinemaExperience cinemaExperience;
    private final List<Seat> seatList;
    private List<Show> showArrayList;

    public Hall(
            String id,
            String name,
            CinemaExperience cinemaExperience,
            List<Seat> seatList,
            List<Show> showList
    ) {
        this.id = id;
        this.name = name;
        this.cinemaExperience = cinemaExperience;
        this.seatList = seatList;
        this.showArrayList = showList;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CinemaExperience getCinemaExperience() {
        return cinemaExperience;
    }

    public List<Seat> getSeatList() {
        return seatList;
    }

    public List<Show> getShowList() {
        return showArrayList;
    }

    public void addShow(Show show) {
        showArrayList.add(show);
        System.out.println("   [Hall] Show for '" + show.getMovie().getName() + "' at " + show.getTime()
                + " added to Hall '" + name + "'");
    }
}