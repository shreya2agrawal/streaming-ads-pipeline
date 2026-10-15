package com.adsplatform.placement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementRepository extends JpaRepository<Placement, Long>{
    List<Placement> findByActiveTrue();
    List<Placement> findByTypeAndActiveTrue(PlacementType type);
    boolean existsByNameIgnoreCase(String name);
    // On update, the placement's own current name must not count as a duplicate.
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
