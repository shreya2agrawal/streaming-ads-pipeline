import client from "./client";
import { Campaign } from "../types";

export const getCampaigns = (): Promise<Campaign[]> =>
    client.get('/campaigns');

export const getCampaign = (id: number): Promise<Campaign> =>
    client.get(`/campaigns/${id}`);

export const createCampaign = (data: Partial<Campaign>): Promise<Campaign> =>
    client.post('/campaigns', data);

export const updateCampaign = (id: number, data: Partial<Campaign>): Promise<Campaign> =>
    client.put(`/campaigns/${id}`, data);

export const deleteCampaign = (id: number): Promise<void> =>
    client.delete(`/campaigns/${id}`);

export const getActiveCount = (): Promise<number> =>
    client.get('/campaigns/stats/active-count');

export const getUnderPacing = (theshold: number = 50): Promise<Campaign[]> => 
    client.get(`/campaigns/under-pacing?threshold=${theshold}`);
