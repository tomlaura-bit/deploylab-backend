package edu.deploylab;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SimulationEngineTest {
    private final SimulationEngine engine = new SimulationEngine();
    @Test void correctActionResolvesIncidentWithFullScore() {
        var result = engine.apply("IN_PROGRESS", "FIX_URL", "FIX_URL", 0, 0);
        assertThat(result.state()).isEqualTo("RESOLVED");
        assertThat(result.score()).isEqualTo(100);
    }
    @Test void wrongActionKeepsIncidentOpen() {
        var result = engine.apply("IN_PROGRESS", "RESTART", "FIX_URL", 0, 0);
        assertThat(result.state()).isEqualTo("IN_PROGRESS");
        assertThat(result.score()).isZero();
    }
    @Test void hintsAndMistakesReduceScoreButNeverBelowZero() {
        assertThat(engine.apply("IN_PROGRESS", "FIX_URL", "FIX_URL", 2, 1).score()).isEqualTo(75);
        assertThat(engine.apply("IN_PROGRESS", "FIX_URL", "FIX_URL", 50, 3).score()).isZero();
    }
    @Test void completedAttemptCannotBeModified() {
        assertThatThrownBy(() -> engine.apply("RESOLVED", "RESTART", "FIX_URL", 0, 0))
            .isInstanceOf(IllegalStateException.class);
    }
}
