package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.ApiTool;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiToolRepository extends JpaRepository<ApiTool, Long> {

    @EntityGraph(attributePaths = { "provider", "parameters" })
    @Query("SELECT a FROM ApiTool a WHERE a.code = :code")
    Optional<ApiTool> findByCode(@Param("code") String code);

    @EntityGraph(attributePaths = { "provider", "parameters" })
    @Query("SELECT a FROM ApiTool a WHERE a.enabled = true")
    List<ApiTool> findAllEnabled();

    @EntityGraph(attributePaths = { "provider", "parameters" })
    @Query("SELECT a FROM ApiTool a")
    List<ApiTool> findAllWithRelations();

    boolean existsByCode(String code);
}
