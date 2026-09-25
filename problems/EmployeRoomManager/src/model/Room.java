package model;

public class Room {
    private final String id;
    private final String name;
    private final int floor;
    private final int capacity;

    public Room(String id, String name, int floor, int capacity) {
        this.id = id;
        this.name = name;
        this.floor = floor;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getFloor() {
        return floor;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return "Room{id=" + id + ", name=" + name + ", floor=" + floor + ", capacity=" + capacity + "}";
    }
}
