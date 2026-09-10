package entity;

import enums.SeatStatus;

public class Seat {
    private String id;
    private String number;
    private String description;
    private SeatStatus status;
    private int noOfBags;
    private int totalWeightAllowance;

    public Seat(String id, String number, String description, SeatStatus status,
                int noOfBags, int totalWeightAllowance) {
        this.id = id;
        this.number = number;
        this.description = description;
        this.status = status;
        this.noOfBags = noOfBags;
        this.totalWeightAllowance = totalWeightAllowance;
    }

    public String getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getDescription() {
        return description;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public int getNoOfBags() {
        return noOfBags;
    }

    public int getTotalWeightAllowance() {
        return totalWeightAllowance;
    }

    public String luggageAllowanceSummary() {
        return "Seat " + number + " (" + description + ") allows " + noOfBags
                + " bag(s) with a total weight limit of " + totalWeightAllowance + " kg";
    }

    @Override
    public String toString() {
        return "Seat[" + number + ", " + description + ", status=" + status
                + ", bags=" + noOfBags + ", weightLimit=" + totalWeightAllowance + "kg]";
    }
}
