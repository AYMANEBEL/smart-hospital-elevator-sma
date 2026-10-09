package com.smartelevator.model;

public enum RequestStatus {
    WAITING("En attente"),
    ASSIGNED("Affectée"),
    RIDING("En trajet"),
    SERVED("Servie");

    private final String label;
    RequestStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
