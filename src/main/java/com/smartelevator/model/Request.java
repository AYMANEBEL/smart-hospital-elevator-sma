package com.smartelevator.model;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final int sourceFloor;
    private final int destinationFloor;
    private final UserType userType;
    private final long createdAt;
    private final int priority;

    public Request(String id, int sourceFloor, int destinationFloor, UserType userType) {
        this(id, sourceFloor, destinationFloor, userType, System.currentTimeMillis(), 0);
    }

    public Request(String id, int sourceFloor, int destinationFloor, UserType userType,
                   long createdAt, int priority) {
        this.id = id;
        this.sourceFloor = sourceFloor;
        this.destinationFloor = destinationFloor;
        this.userType = userType;
        this.createdAt = createdAt;
        this.priority = priority;
    }

    public Request withPriority(int newPriority) {
        return new Request(id, sourceFloor, destinationFloor, userType, createdAt, newPriority);
    }

    public String getId() { return id; }
    public int getSourceFloor() { return sourceFloor; }
    public int getDestinationFloor() { return destinationFloor; }
    public UserType getUserType() { return userType; }
    public long getCreatedAt() { return createdAt; }
    public int getPriority() { return priority; }

    public long waitingSeconds() {
        return Math.max(0, (System.currentTimeMillis() - createdAt) / 1000L);
    }
}
