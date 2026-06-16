package com.adsplatform.safety;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyService{

    private final SafetyFlagRepository flagRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public List<SafetyFlag> getPendingFlags(){
        return flagRepository.findByFlagStatus(SafetyFlag.FlagStatus.PENDING);
    }

    public SafetyFlag createFlag(SafetyFlag safetyFlag){
        SafetyFlag saved = flagRepository.save(safetyFlag);
        String key = "safety:pending:count";
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofHours(24));
        kafkaTemplate.send("safety-flags","FLAG: " + saved.getId() + " : " + saved.getSeverity());
        log.warn("Safety flag created for creative: {}", safetyFlag.getCreativeFile());
        return saved;
    }

    public SafetyFlag reviewFlag(Long id, String reviewer, boolean approved){
        SafetyFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flag not found: " + id));
        flag.setFlagStatus(approved ? SafetyFlag.FlagStatus.APPROVED : SafetyFlag.FlagStatus.REJECTED);
        flag.setReviewedBy(reviewer);
        flag.setReviewedAt(LocalDateTime.now());
        SafetyFlag saved = flagRepository.save(flag);
        redisTemplate.opsForValue().decrement("safety:pending:count");
        return saved;
    }

    public long getPendingCount(){
        String cached = redisTemplate.opsForValue().get("safety:pending:count");
        if(cached != null) return Long.parseLong(cached);
        return flagRepository.countByFlagStatus(SafetyFlag.FlagStatus.PENDING);
    }
}
