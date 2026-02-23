package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.ApiProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiProviderRepository extends JpaRepository<ApiProvider, Long> {
    Optional<ApiProvider> findByName(String name);
}
