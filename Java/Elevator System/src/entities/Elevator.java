package entities;

import java.time.Duration;
import java.util.PriorityQueue;

public class Elevator {
    private String id;
    private String name;
    private int currentFloor;
    private Direction direction;
    private PriorityQueue<CarCall> upRequests;
    private PriorityQueue<CarCall> downRequests;

    public Elevator(String id, String name) {
        this.id = id;
        this.name = name;
        this.currentFloor = 0;
        this.direction = Direction.IDLE;

        //Min Heap: 1 2 3 4 5...
        this.upRequests = new PriorityQueue<>(
                (a, b) -> Integer.compare(a.getFloor(), b.getFloor())
        );

        //Max Heap: 5 4 3 2 1
        this.downRequests = new PriorityQueue<>(
                (a, b) -> Integer.compare(b.getFloor(), a.getFloor())
        );
    }



    public void add(CarCall carCall){
        int requestFloor = carCall.getFloor();

        Direction requestDirection = this.currentFloor == requestFloor
                ? Direction.IDLE
                : (this.currentFloor < requestFloor ? Direction.UP : Direction.DOWN);

        switch (requestDirection) {
            case UP -> this.upRequests.offer(carCall);
            case DOWN -> this.downRequests.offer(carCall);
            default -> System.out.printf("%s at Floor %d: Destination Floor is same as the current floor. Not Allowed%n",
                    this.name,
                    this.currentFloor);
        }

        this.move();
    }

    private void move(){
        if(this.upRequests.isEmpty()){

            if(!this.downRequests.isEmpty()){
                int destinationFloor = this.downRequests.poll().getFloor();
                this.direction = Direction.DOWN;
                moveElevator(destinationFloor);
            }

        }else{
            int destinationFloor = this.upRequests.poll().getFloor();
            this.direction = Direction.UP;
            moveElevator(destinationFloor);
        }

        this.direction = Direction.IDLE;
        System.out.printf("%s at Floor %d: Waiting for future HallCall%n", this.name, this.currentFloor);
    }

    public Direction getDirection() {
        return this.direction;
    }

    public int getCurrentFloor() {
        return this.currentFloor;
    }

    public String getElevatorId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    private void moveElevator(int destinationFloor){
        while(this.currentFloor < destinationFloor){
            System.out.printf("%s going %s: Current Floor %d -> Destination Floor %d%n",
                    this.name,
                    this.direction,
                    currentFloor,
                    destinationFloor);
            try {
                this.currentFloor += 1;
                Thread.sleep(Duration.ofSeconds(1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        while(this.currentFloor > destinationFloor){
            System.out.printf("%s going %s: Current Floor %d -> Destination Floor %d%n",
                    this.name,
                    this.direction,
                    currentFloor,
                    destinationFloor);
            try {
                this.currentFloor -= 1;
                Thread.sleep(Duration.ofSeconds(1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
