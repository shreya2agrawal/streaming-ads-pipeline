import client from "./client";
import { Inventory, InventoryStats } from "../types";

export const getAllInventory = (): Promise<Inventory[]> =>
    client.get('/inventory');

export const getInventoryByShow = (showId: string): Promise<Inventory> =>
    client.get(`/inventory/${showId}`);

export const getAvailableSlots = (showId: string): Promise<number> =>
    client.get(`/inventory/${showId}/available-slots`);

export const getInventoryStats = (): Promise<InventoryStats> =>
    client.get('/inventory/stats');

export const allocateSlots = (
    showId: string,
    slots: number
): Promise<Inventory> =>
    client.post(`/inventory/${showId}/allocate?slots=${slots}`);

export const releaseSlots = (
    showId: string,
    slots: number
): Promise<Inventory> =>
    client.post(`/inventory/${showId}/release?slots=${slots}`);

export const getAvailableInventory = (
    minimumSlots: number = 1
): Promise<Inventory[]> =>
    client.get(`/inventory/available?minimumSlots=${minimumSlots}`);