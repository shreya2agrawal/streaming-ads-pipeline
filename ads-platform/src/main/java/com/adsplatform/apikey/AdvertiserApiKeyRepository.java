package com.adsplatform.apikey;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertiserApiKeyRepository extends JpaRepository<AdvertiserApiKey, Long> {
    
    Optional<AdvertiserApiKey> findByKeyHash(String keyHash);

    List<AdvertiserApiKey> findByAdvertiserOrderByCreatedAtDesc(String advertiser);
}
