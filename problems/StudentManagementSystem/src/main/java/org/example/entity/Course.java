package org.example.entity;

public class Course {

    private final int id;
    private final String name;
    private final String description;
    private final int maxSeats;

    public Course(int id, String name, String description, int maxSeats) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxSeats = maxSeats;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxSeats() {
        return maxSeats;
    }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", maxSeats=" + maxSeats +
                '}';
    }
}