package com.smartelevator.core;

public class MaintenanceCommand {
    private final String elevatorName;
    private final boolean active;

    public MaintenanceCommand(String elevatorName, boolean active) {
        this.elevatorName = elevatorName;
        this.active = active;
    }

    public String getElevatorName() { return elevatorName; }
    public boolean isActive() { return active; }
}
