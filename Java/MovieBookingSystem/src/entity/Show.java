package entity;

import java.time.LocalDateTime;
import java.util.*;

public class Show {
    private final Movie movie;
    private final List<ShowSeat> showSeatList;
    private final LocalDateTime time;

    public Show(
            Movie movie,
            List<ShowSeat> showSeatList,
            LocalDateTime time
    ) {
        this.movie = movie;
        this.showSeatList = showSeatList;
        this.time = time;
    }

    public Movie getMovie() {
        return movie;
    }

    public List<ShowSeat> getShowSeatList() {
        return showSeatList;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
