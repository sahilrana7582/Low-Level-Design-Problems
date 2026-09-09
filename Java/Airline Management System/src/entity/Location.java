package entity;

public class Location {
    private String addressLine;
    private String pinCode;
    private String city;
    private String state;

    public Location(String addressLine, String pinCode, String city, String state) {
        this.addressLine = addressLine;
        this.pinCode = pinCode;
        this.city = city;
        this.state = state;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getPinCode() {
        return pinCode;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    @Override
    public String toString() {
        return addressLine + ", " + city + ", " + state + " - " + pinCode;
    }
}
