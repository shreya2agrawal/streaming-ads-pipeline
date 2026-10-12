package com.adsplatform.creative;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.campaign.Campaign;
import com.adsplatform.campaign.CampaignRepository;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.dto.CreateCreativeRequest;
import com.adsplatform.creative.dto.CreativeResponse;
import com.adsplatform.creative.dto.UpdateCreativeRequest;

@Service
public class CreativeService {
    private final CreativeRepository creativeRepository;
    private final CampaignRepository campaignRepository;

    public CreativeService(
            CreativeRepository creativeRepository,
            CampaignRepository campaignRepository) {
        this.creativeRepository = creativeRepository;
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    public CreativeResponse createCreative(CreateCreativeRequest request) {
        Campaign campaign = campaignRepository.findById(request.campaignId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign not found: " + request.campaignId()));

        validateDuration(request.creativeType(), request.durationSeconds());

        Creative creative = Creative.builder()
                .campaign(campaign)
                .name(request.name())
                .creativeType(request.creativeType())
                .mediaUrl(request.mediaUrl())
                .destinationUrl(request.destinationUrl())
                .contentText(request.contentText())
                .durationSeconds(request.durationSeconds())
                .status(CreativeStatus.DRAFT)
                .safetyStatus(CreativeSafetyStatus.UNREVIEWED)
                .build();

        Creative savedCreative = creativeRepository.save(creative);

        return toResponse(savedCreative);
    }

    @Transactional(readOnly = true)
    public CreativeResponse getCreative(Long creativeId) {
        Creative creative = creativeRepository.findById(creativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creative not found: " + creativeId));

        return toResponse(creative);
    }

    @Transactional
    public CreativeResponse updateCreative(
            Long creativeId,
            UpdateCreativeRequest request) {

        Creative creative = creativeRepository.findById(creativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creative not found: " + creativeId));

        validateDuration(
                request.creativeType(),
                request.durationSeconds());

        creative.setName(request.name());
        creative.setCreativeType(request.creativeType());
        creative.setMediaUrl(request.mediaUrl());
        creative.setDestinationUrl(request.destinationUrl());
        creative.setContentText(request.contentText());
        creative.setDurationSeconds(request.durationSeconds());
        creative.setSafetyStatus(CreativeSafetyStatus.UNREVIEWED);

        Creative savedCreative = creativeRepository.save(creative);

        return toResponse(savedCreative);
    }

    @Transactional
    public CreativeResponse updateStatus(
            Long creativeId,
            CreativeStatus newStatus) {

        Creative creative = creativeRepository.findById(creativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creative not found: " + creativeId));

        creative.setStatus(newStatus);

        Creative savedCreative = creativeRepository.save(creative);

        return toResponse(savedCreative);
    }

    @Transactional(readOnly = true)
    public List<CreativeResponse> getAllCreatives() {
        return creativeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CreativeResponse> getCreativesByCampaign(Long campaignId) {
        return creativeRepository.findByCampaign_Id(campaignId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateDuration(
            CreativeType creativeType,
            Integer durationSeconds) {
        if (creativeType == CreativeType.VIDEO && durationSeconds == null) {
            throw new IllegalArgumentException(
                    "Duration is required for video creatives");
        }
    }

    private CreativeResponse toResponse(Creative creative) {
        return new CreativeResponse(
                creative.getId(),
                creative.getCampaign().getId(),
                creative.getName(),
                creative.getCreativeType(),
                creative.getMediaUrl(),
                creative.getDestinationUrl(),
                creative.getContentText(),
                creative.getDurationSeconds(),
                creative.getStatus(),
                creative.getSafetyStatus(),
                creative.getCreatedAt(),
                creative.getUpdatedAt());
    }
}
