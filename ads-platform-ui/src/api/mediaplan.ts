import client from "./client";
import { MediaPlan } from "../types";

export interface MediaPlanStats {
    draftCount: number,
    activeCount: number,
    approvedCount: number,
    completedCount: number,
    underDeliveringCount: number
}

export const getAllPlans = (): Promise<MediaPlan[]> =>
    client.get('/media-plans');

export const getPlanById = (id: number): Promise<MediaPlan> =>
    client.get(`/media-plans/${id}`);

export const createPlan = (data: Partial<MediaPlan>): Promise<MediaPlan> =>
    client.post('/media-plans', data);

export const updatePlan = (
    id: number,
    data: Partial<MediaPlan>
): Promise<MediaPlan> =>
    client.put(`/media-plans/${id}`, data);

export const sendProposal = (id: number): Promise<MediaPlan> =>
    client.post(`/media-plans/${id}/send-proposal`);

export const approvePlan = (id: number): Promise<MediaPlan> =>
    client.post(`/media-plans/${id}/approve`);

export const negotiatePlan = (id: number): Promise<MediaPlan> =>
    client.post(`/media-plans/${id}/negotiate`);

export const allocateInventory = (id: number): Promise<MediaPlan> =>
    client.post(`/media-plans/${id}/allocate-inventory`);

export const activatePlan = (id: number): Promise<MediaPlan> =>
    client.post(`/media-plans/${id}/activate`);

export const completePlan = (id: number): Promise<MediaPlan> =>
    client.post(`/media-plans/${id}/complete`);

export const cancelPlan = (id: number): Promise<MediaPlan> =>
    client.post(`/media-plans/${id}/cancel`);

export const getPipeline = (): Promise<MediaPlan[]> =>
    client.get('/media-plans/pipeline');

export const getUnderDelivering = (
    threshold: number = 80
): Promise<MediaPlan[]> =>
    client.get(`/media-plans/under-delivering?threshold=${threshold}`);

export const getMediaPlanStats = (): Promise<MediaPlanStats> =>
    client.get('/media-plans/stats');

export const getNextAction = (plan: MediaPlan): { label: string; action: string } | null => {
    switch (plan.status) {
        case 'DRAFT':
            return { label: 'Send proposal', action: 'send-proposal' };
        case 'PROPOSAL_SENT':
            return { label: 'Approve', action: 'approve' };
        case "NEGOTIATING":
            return { label: 'Approve', action: 'approve' };
        case "APPROVED":
            if (plan.totalSlotsAllocated === 0) {
                return { label: 'Allocate inventory', action: 'allocate' };
            }
            if(plan.totalSlotsAllocated > 0){
                return { label: 'Allocate inventory', action: 'allocate' };
            }
            if(plan.fullyAllocated){
                return {label: 'Activate', action: 'activate'};
            }
            return null;
        default:
            return null;
    }
};