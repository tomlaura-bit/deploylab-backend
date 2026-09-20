package edu.deploylab;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SimulationEngineTest {
    private final SimulationEngine engine = new SimulationEngine();
    @Test void correctActionResolvesIncidentWithoutGradingBeforeFinalization() {
        var result = engine.apply("IN_PROGRESS", "FIX_URL", "FIX_URL", 0, 0);
        assertThat(result.state()).isEqualTo("RESOLVED");
        assertThat(result.score()).isZero();
        assertThat(engine.evaluate(result.state(),0,0)).isEqualTo(100);
    }
    @Test void wrongActionKeepsIncidentOpen() {
        var result = engine.apply("IN_PROGRESS", "RESTART", "FIX_URL", 0, 0);
        assertThat(result.state()).isEqualTo("IN_PROGRESS");
        assertThat(result.score()).isZero();
    }
    @Test void hintsAndMistakesReduceScoreButNeverBelowZero() {
        assertThat(engine.evaluate("RESOLVED",2,1)).isEqualTo(75);
        assertThat(engine.evaluate("RESOLVED",50,3)).isZero();
        assertThat(engine.evaluate("IN_PROGRESS",0,0)).isZero();
    }
    @Test void completedAttemptCannotBeModified() {
        assertThatThrownBy(() -> engine.apply("RESOLVED", "RESTART", "FIX_URL", 0, 0))
            .isInstanceOf(IllegalStateException.class);
    }
}
