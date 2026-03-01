package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.AccessToken;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, Long> {

    @Query("SELECT t FROM AccessToken t ORDER BY t.id ASC LIMIT 1")
    Optional<AccessToken> findFirst();
}
