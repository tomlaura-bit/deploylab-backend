package edu.deploylab.persistence;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AppUserRepository extends JpaRepository<AppUserEntity,UUID> {Optional<AppUserEntity> findByEmailIgnoreCase(String email);boolean existsByEmailIgnoreCase(String email);}
