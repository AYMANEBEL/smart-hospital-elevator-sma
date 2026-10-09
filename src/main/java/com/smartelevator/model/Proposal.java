package com.smartelevator.model;

import java.io.Serializable;

public class Proposal implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String elevatorName;
    private final String requestId;
    private final double score;
    private final int distance;
    private final int workload;
    private final int directionPenalty;

    public Proposal(String elevatorName, String requestId, double score,
                    int distance, int workload, int directionPenalty) {
        this.elevatorName = elevatorName;
        this.requestId = requestId;
        this.score = score;
        this.distance = distance;
        this.workload = workload;
        this.directionPenalty = directionPenalty;
    }

    public String getElevatorName() { return elevatorName; }
    public String getRequestId() { return requestId; }
    public double getScore() { return score; }
    public int getDistance() { return distance; }
    public int getWorkload() { return workload; }
    public int getDirectionPenalty() { return directionPenalty; }
}
