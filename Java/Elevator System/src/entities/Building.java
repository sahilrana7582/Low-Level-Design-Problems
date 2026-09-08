package entities;

public class Building {

    private final int floors;
    private final ElevatorController elevatorController;

    public Building(
            int floors,
            int totalElevators,
            DispatchElevatorStrategy dispatchElevatorStrategy
    ) {
        this.floors = floors;
        this.elevatorController = new ElevatorController(
                totalElevators,
                dispatchElevatorStrategy
        );
    }

    public int getFloors() {
        return this.floors;
    }

    public ElevatorController getElevatorController() {
        return this.elevatorController;
    }
}
