package edu.deploylab;

import org.springframework.stereotype.Component;

@Component
public class SimulationEngine {
    public record Outcome(String state, int score, String feedback) {}
    public Outcome apply(String state, String action, String expectedAction, int mistakes, int hints) {
        if (!"IN_PROGRESS".equals(state)) throw new IllegalStateException("El intento ya está resuelto");
        if (action.equals(expectedAction)) {
            return new Outcome("RESOLVED", 0,
                "Incidente resuelto. Finaliza el intento para obtener tu evaluación.");
        }
        return new Outcome("IN_PROGRESS",0,"La acción no resuelve la causa. Revisa las evidencias antes de intentar otra solución.");
    }
    public int evaluate(String state,int mistakes,int hints) {
        if(mistakes<0 || hints<0) throw new IllegalArgumentException("Contadores inválidos");
        if(!state.equals("RESOLVED")) return 0;
        return (int)Math.max(0L,100L-10L*mistakes-5L*hints);
    }
}
