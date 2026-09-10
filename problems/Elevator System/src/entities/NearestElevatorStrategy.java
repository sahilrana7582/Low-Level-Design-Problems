package entities;

import java.util.List;

public class NearestElevatorStrategy implements DispatchElevatorStrategy {

    @Override
    public Elevator getElevator(HallCall hallCall, List<Elevator> elevators) {

        Elevator nearestElevator = null;
        int bestScore = Integer.MIN_VALUE;

        int requestFloor = hallCall.getFloor();
        Direction requestDirection = hallCall.getDirection();

        for (Elevator elevator : elevators) {

            int score = 0;

            score += elevator.getDirection() == requestDirection ? 1 : -1;

            score -= Math.abs(
                    elevator.getCurrentFloor() - requestFloor
            );

            if (score > bestScore) {
                bestScore = score;
                nearestElevator = elevator;
            }
        }

        return nearestElevator;
    }
}