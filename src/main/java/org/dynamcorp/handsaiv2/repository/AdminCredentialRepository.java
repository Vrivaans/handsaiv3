package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.AdminCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminCredentialRepository extends JpaRepository<AdminCredential, Long> {

    @Query("SELECT a FROM AdminCredential a ORDER BY a.id ASC LIMIT 1")
    Optional<AdminCredential> findFirst();

    boolean existsByUsername(String username);
}
