package com.adsplatform.mediaplan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MediaPlanRepository extends JpaRepository<MediaPlan, Long> {

    List<MediaPlan> findByStatus(MediaPlan.PlanStatus status);

    List<MediaPlan> findByAdvertiserContainingIgnoreCase(String advertiser);

    List<MediaPlan> findByPlanType(MediaPlan.PlanType planType);

    @Query("SELECT mp FROM MediaPlan mp WHERE mp.status = 'ACTIVE' " +
    " AND mp.flightEnd < :today")
    List<MediaPlan> findExpiredActivePlans(@Param("today") LocalDate today);

    @Query("SELECT mp FROM MediaPlan mp WHERE mp.status IN ('DRAFT'," +
    "'PROPOSAL_SENT','NEGOTIATING') ORDER BY mp.createdAt DESC")
    List<MediaPlan> findPipelinePlans();

    @Query("SELECT mp FROM MediaPlan mp WHERE " +
            "mp.completionRate < :threshold AND mp.status = 'ACTIVE'")
    List<MediaPlan> findUnderDeliveringPlans(
            @Param("threshold") double threshold);

    long countByStatus(MediaPlan.PlanStatus status);

}
