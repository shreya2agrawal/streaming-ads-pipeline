package com.adsplatform.safety;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "safety_flags")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SafetyFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String creativeFile;

    @Column(nullable = false)
    private String advertiser;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagStatus flagStatus;

    private String reviewedBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        flagStatus = FlagStatus.PENDING;
    }

    public enum Severity {ADVISORY, TIER_3, POLICY_VIOLATION};
    public enum FlagStatus {PENDING, APPROVED, REJECTED};
}
