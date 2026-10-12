package com.adsplatform.creative;

import java.time.LocalDateTime;

import com.adsplatform.campaign.Campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "creatives")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
@Builder 
public class Creative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreativeType creativeType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String destinationUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentText;

    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreativeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreativeSafetyStatus safetyStatus;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist 
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        if(status == null){
            status = CreativeStatus.DRAFT;
        }
        
        if(safetyStatus == null){
            safetyStatus = CreativeSafetyStatus.UNREVIEWED;
        }
    }

    @PreUpdate 
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

}
