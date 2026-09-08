package entities;

public class HallCall {
    private int floor;
    private Direction direction;

    public HallCall(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
    }

    public int getFloor(){
        return this.floor;
    }

    public Direction getDirection() {
        return this.direction;
    }
}
