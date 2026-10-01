package edu.deploylab;

import org.slf4j.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import org.springframework.beans.factory.annotation.Value;

@Component
public class DomainEventListener {
    private static final Logger log=LoggerFactory.getLogger(DomainEventListener.class);
    private final JobService jobs;
    private final boolean mailEnabled;

    public DomainEventListener(JobService jobs,@Value("${app.mail-enabled}") boolean mailEnabled) { this.jobs=jobs;this.mailEnabled=mailEnabled; }

    @Async("applicationTaskExecutor") @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void registered(UserRegisteredEvent event) {
        log.info("event=user_registered userId={} occurredAt={}",event.userId(),event.occurredAt());
        if(mailEnabled) jobs.enqueue(event.userId(),"WELCOME_EMAIL","Tu cuenta quedó registrada. Ya puedes iniciar una simulación desde el catálogo de talleres.");
    }

    @Async("applicationTaskExecutor") @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void assigned(AssignmentCreatedEvent event) {
        log.info("event=assignment_created assignmentId={} groupId={} workshopId={} occurredAt={}",
            event.assignmentId(),event.groupId(),event.workshopId(),event.occurredAt());
    }

    @Async("applicationTaskExecutor") @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void finished(AttemptFinishedEvent event) {
        log.info("event=attempt_finished attemptId={} userId={} score={} solved={} occurredAt={}",
            event.attemptId(),event.userId(),event.score(),event.solved(),event.occurredAt());
    }
}
