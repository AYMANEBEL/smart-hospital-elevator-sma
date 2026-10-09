package com.smartelevator.model;

import java.io.Serializable;

public enum UserType implements Serializable {
    NORMAL("Patient / visiteur", 1),
    ELDERLY("Personne âgée", 2),
    PMR("PMR / Mobilité réduite", 4),
    EMERGENCY("Urgence médicale", 7);

    private final String label;
    private final int basePriority;

    UserType(String label, int basePriority) {
        this.label = label;
        this.basePriority = basePriority;
    }

    public String getLabel() { return label; }
    public int getBasePriority() { return basePriority; }

    @Override
    public String toString() { return label; }
}
