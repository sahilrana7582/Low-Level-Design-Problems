package entity;

import java.util.ArrayList;
import java.util.List;

public class Theater {
    private final String id;
    private final String name;
    private final Location location;
    // Currently we are only focusing on the all the movies
    // Theater is currently running
    // In Future we will we extend that the movies range
    // Like Movie A will be running on 1st 2nd 3rd 5th of April
    // Like the list of the days the movies will be running in that Theater
    private final List<Movie> movies;
    private final List<Hall> hallList;

    public Theater(
            String id,
            String name,
            Location location
    ) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.movies = new ArrayList<>();
        this.hallList = new ArrayList<>();
    }

    public void addMovie(Movie movie) {
        movies.add(movie);
        System.out.println("   [Theater] Movie '" + movie.getName() + "' is now running at " + name);
    }

    public void addHall(Hall hall) {
        hallList.add(hall);
        System.out.println("   [Theater] Hall '" + hall.getName() + "' added to " + name);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public List<Hall> getHallList() {
        return hallList;
    }
}
