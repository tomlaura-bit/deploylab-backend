package edu.deploylab;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DataIntegrityViolationException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="app.extras-enabled=false") @ActiveProfiles("test")
class TransactionRollbackTest {
    @MockitoBean SimulationEngine engine;
    @Autowired AuthService auth;
    @Autowired AttemptService attempts;
    @Autowired Db db;
    @Test void eventInsertFailureRollsBackStateChange() {
        var user=auth.register(new AuthService.Register("Rollback",UUID.randomUUID()+"@test.local","RollbackTest123!"));
        var scenario=Db.id(db.one("SELECT id FROM scenario WHERE template_key='API_URL' AND workshop_id=?",UUID.fromString("10000000-0000-0000-0000-000000000001")),"id");
        UUID id=attempts.start(user,scenario);
        // The state update succeeds, then the too-long feedback violates the real DB column limit.
        when(engine.apply(anyString(),anyString(),anyString(),anyInt(),anyInt()))
            .thenReturn(new SimulationEngine.Outcome("RESOLVED",0,"x".repeat(2001)));
        assertThatThrownBy(()->attempts.action(id,user,UUID.randomUUID(),"FIX_URL"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(db.one("SELECT state,finalized FROM attempt WHERE id=?",id))
            .containsEntry("state","IN_PROGRESS").containsEntry("finalized",false);
        assertThat(db.scalar("SELECT COUNT(*) FROM attempt_event WHERE attempt_id=?",Integer.class,id)).isZero();
    }
}
