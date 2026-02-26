package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.ApiProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;
import java.util.List;

@Repository
public interface ApiProviderRepository extends JpaRepository<ApiProvider, Long> {
    Optional<ApiProvider> findByName(String name);

    Optional<ApiProvider> findByCode(String code);

    @EntityGraph(attributePaths = { "tools", "tools.parameters" })
    List<ApiProvider> findByIsExportableTrue();

    @EntityGraph(attributePaths = { "tools", "tools.parameters" })
    List<ApiProvider> findByIdInAndIsExportableTrue(List<Long> ids);
}
