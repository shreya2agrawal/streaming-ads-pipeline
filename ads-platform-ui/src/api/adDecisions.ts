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
 * The backend returns click and impression URLs as paths
 * (/api/v1/...). Resolve them against the API's host, so the
 * backend address lives in one place.
 */
export const toAbsoluteApiUrl = (path: string): string =>
  new URL(path, client.defaults.baseURL).toString();

/*
 * Tells the platform the ad actually appeared. Uses fetch rather than
 * the shared client: the response is an empty 204, and the client's
 * interceptor expects a JSON body. A failed report must never
 * interrupt the ad, so errors are ignored.
 */
export const reportImpression = (impressionUrl: string): void => {
  fetch(toAbsoluteApiUrl(impressionUrl), { method: 'POST' }).catch(() => {
    // Measurement is best-effort; the viewer's experience comes first.
  });
};