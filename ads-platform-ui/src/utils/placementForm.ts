import type { CreativeType, PlacementRequest } from '../types';

export const DEFAULT_MAX_AD_DURATION_SECONDS = 30;

export function acceptsVideo(creativeTypes: CreativeType[]): boolean {
    return creativeTypes.includes('VIDEO');
}

/*
 * Toggles a creative type and keeps the form consistent with the
 * backend rule: a max ad duration exists only when the placement
 * accepts VIDEO. The UI follows the rule instead of letting the
 * user build a request the backend will reject.
 */
export function toggleCreativeType(
    form: PlacementRequest,
    creativeType: CreativeType
): PlacementRequest {
    const isSelected = form.supportedCreativeTypes.includes(creativeType);

    const supportedCreativeTypes = isSelected
        ? form.supportedCreativeTypes.filter((type) => type !== creativeType)
        : [...form.supportedCreativeTypes, creativeType];

    const maxAdDurationSeconds = acceptsVideo(supportedCreativeTypes)
        ? form.maxAdDurationSeconds ?? DEFAULT_MAX_AD_DURATION_SECONDS
        : null;

    return {
        ...form,
        supportedCreativeTypes,
        maxAdDurationSeconds,
    };
}