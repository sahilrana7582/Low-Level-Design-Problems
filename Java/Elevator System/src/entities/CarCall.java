package entities;

public class CarCall {
    private String elevatorId;
    private int destinationFloor;

    public CarCall(String elevatorId, int destinationFloor) {
        this.elevatorId = elevatorId;
        this.destinationFloor = destinationFloor;
    }

    public int getFloor(){
        return this.destinationFloor;
    }
}
