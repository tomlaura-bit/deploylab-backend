package edu.deploylab.persistence;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<NotificationEntity,UUID> {}
