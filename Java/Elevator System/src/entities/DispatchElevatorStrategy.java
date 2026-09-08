package entities;

import java.util.List;

public interface DispatchElevatorStrategy {
    Elevator getElevator(HallCall hallCall, List<Elevator> elevator);
}
