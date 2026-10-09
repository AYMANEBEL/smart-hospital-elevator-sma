package com.smartelevator.model;

public enum Direction {
    UP("↑"), DOWN("↓"), IDLE("•");
    private final String symbol;
    Direction(String symbol) { this.symbol = symbol; }
    public String getSymbol() { return symbol; }
}
