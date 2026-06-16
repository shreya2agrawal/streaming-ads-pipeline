package com.adsplatform.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String showId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false)
    private String contentRating;

    @Column(nullable = false)
    private Long totalSlots;

    @Column(nullable = false)
    private Long allocatedSlots;

    @Column(nullable = false)
    private Long reservedSlots;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if(status == null) status = InventoryStatus.AVAILABLE;
        if(allocatedSlots == null) allocatedSlots = 0L;
        if(reservedSlots == null) reservedSlots = 0L;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
        recalculateStatus();
    }

    public Long getAvailableSlots(){
        return totalSlots - allocatedSlots - reservedSlots;
    }

    public double getFillRate(){
        if (totalSlots == 0) return 0.0;
        return (double) (allocatedSlots + reservedSlots)/totalSlots * 100;
    }

    private void recalculateStatus(){
        double fillRate = getFillRate();
        if(fillRate >= 95.0){
            status = InventoryStatus.SOLD_OUT;
        } else if (fillRate >= 75.0) {
            status = InventoryStatus.NEAR_SOLD_OUT;
        } else {
            status = InventoryStatus.AVAILABLE;
        }
    }

    public enum InventoryStatus {AVAILABLE, NEAR_SOLD_OUT, SOLD_OUT}

}
