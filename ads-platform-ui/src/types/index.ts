export interface Campaign {
    id: number;
    name: string;
    advertiser: string;
    agency: string;
    budget: number;
    flightStart: string;
    flightEnd: string;
    status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'AT_RISK' | 'PACING_LOW';
    pacingPercent: number;
    impressionsDelivered: number;
    impressionsGoal: number;
    createdAt: string;
    updatedAt: string;
}

export interface SafetyFlag {
    id: number;
    creativeFile: string;
    advertiser: string;
    reason: string;
    severity: 'ADVISORY' | 'TIER_3' | 'POLICY_VIOLATION';
    flagStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
    reviewedBy: string | null;
    createdAt: string;
    reviewedAt: string | null;
}

export interface Inventory {
    id: number;
    showId: string;
    title: string;
    genre: string;
    contentRating: string;
    totalSlots: number;
    allocatedSlots: number;
    reservedSlots: number;
    status: 'AVAILABLE' | 'NEAR_SOLD_OUT' | 'SOLD_OUT';
    fillRate: number;
    availableSlots: number;
    createdAt: string;
    updatedAt: string;
}

export interface MediaPlan {
    id: number;
    name: string;
    advertiser: string;
    agency: string;
    totalBudget: number;
    flightStart: string;
    flightEnd: string;
    status: 'DRAFT' | 'PROPOSAL_SENT' | 'NEGOTIATING' | 'APPROVED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
    planType: 'UPFRONT' | 'PROGRAMMATIC' | 'DIRECT' | 'RFP';
    totalSlotsAllocated: number;
    totalSlotsRequested: number;
    completionRate: number;
    targetShowIds: string[];
    targetingSegments: string[];
    rfpReference: string | null;
    notes: string | null;
    createdAt: string;
    updatedAt: string;
    activatedAt: string | null;
    remainingSlots: number;
    fullyAllocated: boolean;
}

export interface InventoryStats {
    totalSlots: number;
    allocatedSlots: number;
    availableSlots: number;
    overallFillRate: number;
}

export interface ApiResponse<T> {
    success: boolean;
    message: string,
    data: T;
}

export type PlacementType =
    | 'PRE_ROLL'
    | 'MID_ROLL'
    | 'POST_ROLL'
    | 'PAUSE_SCREEN'
    | 'HOME_HERO'
    | 'HOME_FEED'
    | 'SEARCH_RESULTS'
    | 'CONTENT_DETAILS'
    | 'LIVE_EVENT_BREAK';

export type DeviceType =
    | 'TV'
    | 'MOBILE'
    | 'WEB'
    | 'TABLET';

export type CreativeType =
    | 'VIDEO'
    | 'IMAGE'
    | 'NATIVE';

export interface Placement {
    id: number;
    name: string;
    type: PlacementType;
    active: boolean;
    maxAds: number | null;
    durationSeconds: number | null;
    supportedDevices: DeviceType[];
    supportedCreativeTypes: CreativeType[];
}

export interface PlacementRequest {
    name: string;
    type: PlacementType;
    maxAds: number | null;
    durationSeconds: number | null;
    supportedDevices: DeviceType[];
    supportedCreativeTypes: CreativeType[];
}

export type CreativeStatus =
  | 'DRAFT'
  | 'ACTIVE'
  | 'PAUSED'
  | 'ARCHIVED';

export type CreativeSafetyStatus =
  | 'UNREVIEWED'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'REJECTED';

export interface Creative {
  id: number;
  campaignId: number;
  name: string;
  creativeType: CreativeType;
  mediaUrl: string;
  destinationUrl: string;
  contentText: string;
  durationSeconds: number | null;
  status: CreativeStatus;
  safetyStatus: CreativeSafetyStatus;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface CreateCreativeRequest {
  campaignId: number;
  name: string;
  creativeType: CreativeType;
  mediaUrl: string;
  destinationUrl: string;
  contentText: string;
  durationSeconds: number | null;
}

export interface UpdateCreativeRequest {
  name: string;
  creativeType: CreativeType;
  mediaUrl: string;
  destinationUrl: string;
  contentText: string;
  durationSeconds: number | null;
}

export type SafetyReviewStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED';

export type SafetyReviewSource =
  | 'HUMAN'
  | 'AUTOMATED';

export interface CreativeSafetyReview {
  id: number;
  creativeId: number;
  status: SafetyReviewStatus;
  reviewSource: SafetyReviewSource;
  reason: string | null;
  score: number | null;
  modelVersion: string | null;
  policyVersion: string | null;
  reviewer: string | null;
  createdAt: string | null;
  reviewedAt: string | null;
}

export interface SafetyReviewDecisionRequest {
  status: SafetyReviewStatus;
  reviewer: string;
  reason: string | null;
}