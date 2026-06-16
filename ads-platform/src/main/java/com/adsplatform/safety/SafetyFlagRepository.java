package com.adsplatform.safety;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SafetyFlagRepository extends JpaRepository<SafetyFlag, Long> {
    List<SafetyFlag> findByFlagStatus(SafetyFlag.FlagStatus status);
    long countByFlagStatus(SafetyFlag.FlagStatus status);
}
