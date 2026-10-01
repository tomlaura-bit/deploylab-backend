package edu.deploylab.persistence;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MaterialRepository extends JpaRepository<MaterialEntity,UUID> {}
