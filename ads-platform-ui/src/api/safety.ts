import client from "./client";
import { SafetyFlag } from "../types";

export const getSafetyFlags = (): Promise<SafetyFlag[]> =>
    client.get('/safety/flags');

export const getPendingCount = (): Promise<number> =>
    client.get('/safety/flags/pending-count');

export const createFlag = (data: Partial<SafetyFlag>): Promise<SafetyFlag> =>
    client.post('/safety/flags', data);

export const reviewFlag = (
    id: number,
    reviewer: string,
    approved: boolean
): Promise<SafetyFlag> =>
    client.patch(`/safety/flags/${id}/review?reviewer=${reviewer}&approved=${approved}`);

export const scoreCreative = (
    creativeText: string,
    advertiser: string,
    targetShowId?: string
): Promise<{
    decision: 'SAFE' | 'REVIEW' | 'BLOCK';
    score: number;
    reason: string;
    topMatchTitle: string;
}> =>
    client.post(
        `/safety/score?creativeText=${encodeURIComponent(creativeText)}&advertiser=${encodeURIComponent(advertiser)}${targetShowId ? `&targetShowId=${targetShowId}` : ''}`
    );