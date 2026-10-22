    package com.adsplatform.campaign;

    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Lock;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;

    import jakarta.persistence.LockModeType;

    import java.util.List;
    import java.util.Optional;

    public interface CampaignRepository extends JpaRepository<Campaign, Long> {
        List<Campaign> findByStatus(Campaign.CampaignStatus status);
        List<Campaign> findByAdvertiserContainingIgnoreCase(String adviser);
        @Query("SELECT c FROM Campaign c WHERE c.pacingPercent < :threshold AND c.status = 'ACTIVE'")
        List<Campaign> findUnderPacing(@Param("threshold") double threshold);
        long countByStatus(Campaign.CampaignStatus status);
        
        /*
        * Loads the campaign with a row lock (SELECT ... FOR UPDATE).
        * Another transaction locking the same campaign waits until this
        * one commits, so two concurrent charges cannot both read the old spend.
        */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT c FROM Campaign c WHERE c.id = :id")
        Optional<Campaign> findByIdForUpdate(@Param("id") Long id);

        // Exact match: API key ownership compares advertiser names exactly.
        boolean existsByAdvertiser(String advertiser);
    }