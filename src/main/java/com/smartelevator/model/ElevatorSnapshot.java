package com.smartelevator.model;

public class ElevatorSnapshot {
    private final String name;
    private final int floor;
    private final Direction direction;
    private final ElevatorStatus status;
    private final int queueSize;
    private final String currentRequestId;
    private final int occupancy;

    public ElevatorSnapshot(String name, int floor, Direction direction, ElevatorStatus status,
                            int queueSize, String currentRequestId, int occupancy) {
        this.name = name;
        this.floor = floor;
        this.direction = direction;
        this.status = status;
        this.queueSize = queueSize;
        this.currentRequestId = currentRequestId;
        this.occupancy = occupancy;
    }

    public String getName() { return name; }
    public int getFloor() { return floor; }
    public Direction getDirection() { return direction; }
    public ElevatorStatus getStatus() { return status; }
    public int getQueueSize() { return queueSize; }
    public String getCurrentRequestId() { return currentRequestId; }
    public int getOccupancy() { return occupancy; }
}
