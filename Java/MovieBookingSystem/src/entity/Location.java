package entity;

import java.util.Objects;

public class Location implements Comparable<Location> {
    private final String address;
    private final String city;
    private final String state;
    private final String postalCode;

    public Location(
            String address,
            String city,
            String state,
            String postalCode
    ) {
        this.address = address;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    @Override
    public int compareTo(Location other) {
        int cityCompare = this.city.compareTo(other.city);
        if (cityCompare != 0) {
            return cityCompare;
        }
        return this.address.compareTo(other.address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location other)) return false;
        return Objects.equals(address, other.address) && Objects.equals(city, other.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, city);
    }

    @Override
    public String toString() {
        return address + ", " + city + ", " + state + " - " + postalCode;
    }
}
