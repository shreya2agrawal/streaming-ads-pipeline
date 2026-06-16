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