package edu.deploylab;

import org.springframework.stereotype.Component;

@Component
public class SimulationEngine {
    public record Outcome(String state, int score, String feedback) {}
    public Outcome apply(String state, String action, String expectedAction, int mistakes, int hints) {
        throw new UnsupportedOperationException("Motor pendiente de implementar");
    }
}
