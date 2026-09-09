package entity;

import enums.MovieCategory;

import java.time.Duration;

public class Movie {
    private final String id;
    private final String name;
    private final Duration duration;
    private final float imdbRating;
    private final MovieCategory movieCategory;   // Or Even we can have the Hashmap of tags.

    public Movie(
            String id,
            String name,
            Duration duration,
            float imdbRating,
            MovieCategory movieCategory
    ) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.imdbRating = imdbRating;
        this.movieCategory = movieCategory;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Duration getDuration() {
        return duration;
    }

    public float getImdbRating() {
        return imdbRating;
    }

    public MovieCategory getMovieCategory() {
        return movieCategory;
    }
}
