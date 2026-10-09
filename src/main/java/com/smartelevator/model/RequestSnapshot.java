package com.smartelevator.model;

public class RequestSnapshot {
    private final Request request;
    private final RequestStatus status;
    private final String elevatorName;
    private final long pickupAt;
    private final long completedAt;

    public RequestSnapshot(Request request, RequestStatus status, String elevatorName,
                           long pickupAt, long completedAt) {
        this.request = request;
        this.status = status;
        this.elevatorName = elevatorName;
        this.pickupAt = pickupAt;
        this.completedAt = completedAt;
    }

    public Request getRequest() { return request; }
    public RequestStatus getStatus() { return status; }
    public String getElevatorName() { return elevatorName; }
    public long getPickupAt() { return pickupAt; }
    public long getCompletedAt() { return completedAt; }

    public double getWaitingSeconds() {
        long end = pickupAt > 0 ? pickupAt : System.currentTimeMillis();
        return Math.max(0, (end - request.getCreatedAt()) / 1000.0);
    }
}
