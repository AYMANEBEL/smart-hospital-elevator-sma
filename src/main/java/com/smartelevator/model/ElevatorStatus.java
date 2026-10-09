package com.smartelevator.model;

public enum ElevatorStatus {
    AVAILABLE("Disponible"),
    BUSY("Occupé"),
    OUT_OF_SERVICE("En panne");

    private final String label;
    ElevatorStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
