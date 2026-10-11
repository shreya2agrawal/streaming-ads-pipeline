package com.adsplatform.placement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementRepository extends JpaRepository<Placement, Long>{
    List<Placement> findByActiveTrue();
    List<Placement> findByTypeAndActiveTrue(PlacementType type);
}
