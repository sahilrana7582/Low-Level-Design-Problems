package entities;

import java.util.ArrayList;
import java.util.List;

public class ElevatorController {

    private final List<Elevator> elevatorList;
    private final List<HallCall> hallCallList;
    private final DispatchElevatorStrategy dispatchElevatorStrategy;

    public ElevatorController(
            int totalElevators,
            DispatchElevatorStrategy dispatchElevatorStrategy
    ) {
        this.elevatorList = new ArrayList<>();
        this.hallCallList = new ArrayList<>();
        this.dispatchElevatorStrategy = dispatchElevatorStrategy;

        for (int i = 0; i < totalElevators; i++) {
            Elevator elevator = new Elevator(
                    Integer.toString(i),
                    String.format("Elevator-%d", i)
            );

            this.elevatorList.add(elevator);
        }
    }

    public void addHallCall(HallCall hallCall) {
        hallCallList.add(hallCall);

        Elevator elevator = dispatchElevatorStrategy.getElevator(
                hallCall,
                elevatorList
        );

        if (elevator != null) {
            CarCall carCall = new CarCall(elevator.getElevatorId(), hallCall.getFloor());
            elevator.add(carCall);
        }
    }

    public List<Elevator> getElevatorList() {
        return elevatorList;
    }

    public List<HallCall> getHallCallList() {
        return hallCallList;
    }
}