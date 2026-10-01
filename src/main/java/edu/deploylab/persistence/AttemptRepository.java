package edu.deploylab.persistence;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface AttemptRepository extends JpaRepository<AttemptEntity,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from AttemptEntity a where a.id=:id and a.user.id=:userId") Optional<AttemptEntity> findOwnedForUpdate(@Param("id") UUID id,@Param("userId") UUID userId);
}
