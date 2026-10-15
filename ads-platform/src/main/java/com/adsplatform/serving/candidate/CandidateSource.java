package com.adsplatform.serving.candidate;

import java.util.List;

import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

public interface CandidateSource {
    List<CandidateAd> findCandidates(AdRequestContext context);
}
