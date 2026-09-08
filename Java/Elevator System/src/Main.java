import entities.Building;
import entities.Direction;
import entities.Elevator;
import entities.ElevatorController;
import entities.HallCall;
import entities.NearestElevatorStrategy;

import java.time.Duration;
import java.util.List;

public class Main {

    private static final int TOTAL_FLOORS = 10;
    private static final int TOTAL_ELEVATORS = 3;

    public static void main(String[] args) throws InterruptedException {

        Building building = new Building(
                TOTAL_FLOORS,
                TOTAL_ELEVATORS,
                new NearestElevatorStrategy()
        );

        ElevatorController elevatorController = building.getElevatorController();

        System.out.printf("Building ready: %d floors, %d elevators%n",
                building.getFloors(),
                elevatorController.getElevatorList().size());

        printElevatorStatus(elevatorController);

        List<HallCall> hallCalls = List.of(
                new HallCall(4, Direction.UP),
                new HallCall(7, Direction.UP),
                new HallCall(2, Direction.DOWN),
                new HallCall(9, Direction.DOWN),
                new HallCall(0, Direction.UP)
        );

        for (HallCall hallCall : hallCalls) {
            System.out.printf("%n--- HallCall: Floor %d, going %s ---%n",
                    hallCall.getFloor(),
                    hallCall.getDirection());

            elevatorController.addHallCall(hallCall);

            printElevatorStatus(elevatorController);

            Thread.sleep(Duration.ofSeconds(1));
        }

        System.out.printf("%nSimulation finished. Total HallCalls served: %d%n",
                elevatorController.getHallCallList().size());
    }

    private static void printElevatorStatus(ElevatorController elevatorController) {
        System.out.println("Status:");

        for (Elevator elevator : elevatorController.getElevatorList()) {
            System.out.printf("  %s -> Floor %d [%s]%n",
                    elevator.getName(),
                    elevator.getCurrentFloor(),
                    elevator.getDirection());
        }
    }
}
