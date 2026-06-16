package com.adsplatform.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByShowId(String showId);

    List<Inventory> findByStatus(Inventory.InventoryStatus status);

    List<Inventory> findByGenreContainingIgnoreCase(String genre);

    @Query("SELECT i FROM Inventory i WHERE "+
    "(i.totalSlots - i.allocatedSlots - i.reservedSlots) >= :requiredSlots")
    List<Inventory> findWithAvailableSlots(@Param("requiredSlots") Long requiredSlots);

    @Query("SELECT i FROM Inventory i ORDER BY " +
    "(CAST(i.allocatedSlots AS double) / i.totalSlots) DESC")
    List<Inventory> findAllOrderByFillRateDesc();
}
