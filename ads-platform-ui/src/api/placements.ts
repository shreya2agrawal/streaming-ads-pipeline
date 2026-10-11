import client from "./client";
import { Placement, PlacementRequest, PlacementType } from "../types";

export const getPlacements = (): Promise<Placement[]> =>
    client.get('/placements');

export const getPlacement = (id: number): Promise<Placement> =>
    client.get(`/placements/${id}`);

export const getActivePlacements = (): Promise<Placement[]> =>
    client.get('/placements/active');

export const getActivePlacementsByType = (type: PlacementType): Promise<Placement[]> =>
    client.get(`/placements/active/type/${type}`);

export const createPlacement = (data: PlacementRequest): Promise<Placement> =>
    client.post('/placements', data);

export const updatePlacement = (id: number, data: PlacementRequest): Promise<Placement> =>
    client.put(`/placements/${id}`, data);

export const updatePlacementActiveStatus = (
    id: number,
    active: boolean
): Promise<Placement> =>
    client.patch(`/placements/${id}/active?active=${active}`);