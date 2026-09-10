package entities;

public abstract class Vehicle {

    private final String companyName;
    private final int yearModel;
    private final VehicleCategory vehicleCategory;

    public Vehicle(
            String companyName,
            int yearModel,
            VehicleCategory category) {

        this.companyName = companyName;
        this.yearModel = yearModel;
        this.vehicleCategory = category;
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getYearModel() {
        return yearModel;
    }

    public VehicleCategory getVehicleCategory() {
        return vehicleCategory;
    }

    public abstract boolean checkAvailability(Booking booking);

    public abstract void confirmBooking(Booking booking);

    public abstract void cancelBooking(Booking booking);
}
