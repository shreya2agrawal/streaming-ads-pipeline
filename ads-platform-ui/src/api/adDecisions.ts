import client from './client';
import type { AdDecisionRequest, AdDecisionResponse } from '../types/adDecision';

/*
 * The client's base URL already ends in /api, so this calls
 * /api/v1/ad-decisions. debug adds the decision explanation.
 */
export const requestAdDecision = (
  request: AdDecisionRequest,
  debug = false
): Promise<AdDecisionResponse> =>
  client.post(`/v1/ad-decisions${debug ? '?debug=true' : ''}`, request);

/*
 * Click URLs come back as paths (/api/v1/clicks/...). Resolve them
 * against the API's host, so the backend address lives in one place.
 */
export const toAbsoluteClickUrl = (clickUrl: string): string =>
  new URL(clickUrl, client.defaults.baseURL).toString();