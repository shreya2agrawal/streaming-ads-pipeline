import type { CreativeType, DeviceType } from './index';

/*
 * Mirrors the backend's /api/v1/ad-decisions contract.
 */
export interface AdDecisionRequest {
  requestId?: string;
  placementId: number;
  deviceType: DeviceType;
  showId: string;
  viewerTrackId?: string;
  audienceSegments?: string[];
  availableSlots: number;
  viewerTimeZone?: string;
}

export interface ServedAd {
  slot: number;
  campaignId: number;
  creativeId: number;
  creativeType: CreativeType;
  mediaUrl: string;
  durationSeconds: number | null;
  clickUrl: string;
}

export interface ScoredAd {
  slot: number;
  campaignId: number;
  creativeId: number;
  priceCpc: number;
  predictedCtr: number;
  score: number;
}

export interface Exclusion {
  campaignId: number;
  creativeId: number;
  reasonCode: string;
  reason: string;
}

export interface DecisionExplanation {
  candidateCount: number;
  winners: ScoredAd[];
  excluded: Exclusion[];
}

export interface AdDecisionResponse {
  decisionId: string;
  ads: ServedAd[];
  noFillReason?: string;
  explanation?: DecisionExplanation;
}