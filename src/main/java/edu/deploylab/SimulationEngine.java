package edu.deploylab;

import org.springframework.stereotype.Component;

@Component
public class SimulationEngine {
    public record Outcome(String state, int score, String feedback) {}
    public Outcome apply(String state, String action, String expectedAction, int mistakes, int hints) {
        if (!"IN_PROGRESS".equals(state)) throw new IllegalStateException("El intento ya está resuelto");
        if (action.equals(expectedAction)) {
            return new Outcome("RESOLVED", Math.max(0,100-mistakes*10-hints*5),
                "Incidente resuelto. Consulta la explicación para revisar el diagnóstico.");
        }
        return new Outcome("IN_PROGRESS",0,"La acción no resuelve la causa. Revisa las evidencias antes de intentar otra solución.");
    }
}
