import apiClient from './client';

import {
  CreativeSafetyReview,
  SafetyReviewDecisionRequest,
} from '../types';

export const submitCreativeForSafetyReview = async (
  creativeId: number
): Promise<CreativeSafetyReview> => {
  return apiClient.post(`/safety-reviews/creatives/${creativeId}`);
};

export const decideSafetyReview = async (
  reviewId: number,
  request: SafetyReviewDecisionRequest
): Promise<CreativeSafetyReview> => {
  return apiClient.patch(`/safety-reviews/${reviewId}`, request);
};

export const getSafetyReviewsForCreative = async (
  creativeId: number
): Promise<CreativeSafetyReview[]> => {
  return apiClient.get(`/safety-reviews/creatives/${creativeId}`);
};