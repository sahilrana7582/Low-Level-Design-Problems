package entities;

public class Location {

    private final String addressLine;
    private final String city;
    private final String state;
    private final String pincode;

    public Location(String addressLine, String city, String state, String pincode) {
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }
}
