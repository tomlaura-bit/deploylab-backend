package edu.deploylab.persistence;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WorkshopRepository extends JpaRepository<WorkshopEntity,UUID> {}
