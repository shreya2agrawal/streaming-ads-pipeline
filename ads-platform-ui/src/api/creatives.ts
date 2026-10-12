import apiClient from './client';
import {
  CreateCreativeRequest,
  Creative,
  CreativeStatus,
  UpdateCreativeRequest,
} from '../types';

export const getCreatives = async (): Promise<Creative[]> => {
    return apiClient.get('/creatives');
};

export const getCreative = async (id: number): Promise<Creative> => {
    return apiClient.get(`/creatives/${id}`);
};

export const getCreativesByCampaign = async (campaignId: number): Promise<Creative[]> => {
    return apiClient.get(`/creatives/campaign/${campaignId}`);
};

export const createCreative = async (request: CreateCreativeRequest): Promise<Creative> => {
    return apiClient.post('/creatives', request);
};

export const updateCreative = async (id: number, request: UpdateCreativeRequest): Promise<Creative> => {
    return apiClient.put(`/creatives/${id}`, request);
};

export const updateCreativeStatus = async (id: number, status: CreativeStatus): Promise<Creative> => {
    return apiClient.patch(`/creatives/${id}/status`,null, { params: { status } });
};
