import React, { useState, useEffect } from 'react';
import MetricCard from '../components/MetricCard';
import StatusBadge from '../components/StatusBadge';
import { getUnderDelivering, getMediaPlanStats } from '../api/mediaplan';
import { getInventoryStats } from '../api/inventory';
import { MediaPlan, InventoryStats } from '../types';
import { MediaPlanStats } from '../api/mediaplan';

export default function InsightsPage() {
    const [underDelivering, setUnderDelivering] = useState<MediaPlan[]>([]);
    const [planStats, setPlanStats] = useState<MediaPlanStats | null>(null);
    const [inventoryStats, setInventoryStats] = useState<InventoryStats | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            getUnderDelivering(80),
            getMediaPlanStats(),
            getInventoryStats(),
        ]).then(([ud, ps, is]) => {
            setUnderDelivering(ud);
            setPlanStats(ps);
            setInventoryStats(is);
        }).finally(() => setLoading(false));
    }, []);

    if (loading) return (
        <div style={{ padding: 24, color: '#888', fontSize: 13 }}>Loading...</div>
    )

    // Generate dynamic recommendations based on real data
    const recommendations: {
        icon: string;
        title: string;
        description: string;
        type: 'warning' | 'info' | 'success';
    }[] = [];

    if (underDelivering.length > 0) {
        underDelivering.forEach(plan => {
            recommendations.push({
                icon: '📉',
                title: `${plan.name} is under-delivering`,
                description: `Completion rate is ${plan.completionRate.toFixed(0)}% - consider reallocation budget or extending the flight date.`,
                type: 'warning',
            });
        });
    }

    if (inventoryStats && inventoryStats.overallFillRate > 85) {
        recommendations.push({
            icon: '📦',
            title: 'Inventory running low',
            description: `Overall fill rate is ${inventoryStats.overallFillRate.toFixed(0)}%. Consider onboarding new titles or upselling existing advertisers on available slots.`,
            type: 'warning',
        });
    }

    if (planStats && planStats.approvedCount > 0) {
        recommendations.push({
            icon: '🚀',
            title: `${planStats.approvedCount} plan(s) ready to activate`,
            description: `You have approved plans waiting for activation. Activate them to start delivering impressions.`,
            type: 'info',
        });
    }

    // Static realistic recommendations
    const staticRecommendations = [
        {
            icon: '🎯',
            title: 'Squid Game S3 — upsell opportunity',
            description: 'High-intent audience overlap with 3 current RFPs in pipeline. 36K slots still available — present to Samsung and Amazon accounts.',
            type: 'success' as const,
        },
        {
            icon: '💡',
            title: 'Shift drama-affinity campaigns to The Crown',
            description: 'The Crown has 170K available slots and strong completion rates (+12% vs average). Ideal for luxury and finance advertisers.',
            type: 'info' as const,
        },
        {
            icon: '⚡',
            title: 'Add frequency cap to binge-watcher segment',
            description: 'Users in binge-watcher segment are seeing avg 6.2 ads per session — above the 4.0 target. Reduce frequency cap to improve completion rates.',
            type: 'warning' as const,
        },
    ];

    const allRecommendations = [...recommendations, ...staticRecommendations];

    const typeColor = {
        warning: { bg: '#451a03', color: '#fbbf24', border: '#78350f' },
        info: { bg: '#0c2340', color: '#60a5fa', border: '#1e3a5f' },
        success: { bg: '#14532d', color: '#4ade80', border: '#166534' },
    };

    return (
        <div style={{ padding: 24 }}>
            {/* Top bar */}
            <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 20,
            }}>
                <h1 style={{ fontSize: 18, fontWeight: 500, color: '#e5e5e5' }}>
                    Insights &amp; optimization
                </h1>
                <span style={{
                    fontSize: 11,
                    padding: '3px 10px',
                    borderRadius: 99,
                    background: '#0c2340',
                    color: '#60a5fa',
                }}>
                    {allRecommendations.length} recommendations
                </span>
            </div>

            {/* Metrics */}
            {planStats && inventoryStats && (
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(4, 1fr)',
                    gap: 12,
                    marginBottom: 24,
                }}>
                    <MetricCard
                        label="Active plans"
                        value={planStats.activeCount}
                        delta='Live now'
                        deltaType='up'
                    />
                    <MetricCard
                        label='Under-delivering'
                        value={underDelivering.length}
                        delta={underDelivering.length > 0 ? 'Needs Attention' : 'All on track'}
                        deltaType={underDelivering.length > 0 ? 'down' : 'up'}
                    />
                    <MetricCard
                        label='Inventory fill rate'
                        value={`${inventoryStats.overallFillRate.toFixed(0)}%`}
                        delta={inventoryStats.overallFillRate > 85 ? 'Running low' : 'Healthy'}
                        deltaType={inventoryStats.overallFillRate > 85 ? 'down' : 'up'}
                    />
                    <MetricCard
                        label="Available slots"
                        value={`${(inventoryStats.availableSlots / 1_000).toFixed(0)}K`}
                        delta="Open for booking"
                        deltaType="neutral"
                    />
                </div>
            )}

            {/* Recommendations */}

            <div style={{
                background: '#141414',
                border: '0.5px solid #1e1e1e',
                borderRadius: 12,
                padding: '14px 16px',
                marginBottom: 16,
            }}>
                <div style={{
                    fontSize: 13,
                    fontWeight: 500,
                    color: '#e5e5e5',
                    marginBottom: 12,
                }}>
                    Recommendations
                </div>
                {allRecommendations.map((rec, i) => {
                    const colors = typeColor[rec.type];
                    return (
                        <div key={i} style={{
                            display: 'flex',
                            alignItems: 'flex-start',
                            gap: 12,
                            padding: '12px 0',
                            borderBottom: i < allRecommendations.length - 1
                                ? '0.5px solid #1e1e1e' : 'none',
                        }}>
                            <div style={{ fontSize: 20 }}>{rec.icon}</div>
                            <div style={{ flex: 1 }}>
                                <div style={{
                                    fontSize: 13,
                                    fontWeight: 500,
                                    color: '#e5e5e5',
                                    marginBottom: 3,
                                }}>
                                    {rec.title}
                                </div>
                                <div style={{ fontSize: 12, color: '#888' }}>
                                    {rec.description}
                                </div>
                            </div>
                            <span style={{
                                fontSize: 11,
                                padding: '2px 10px',
                                borderRadius: 99,
                                background: colors.bg,
                                color: colors.color,
                                border: `0.5px solid ${colors.border}`,
                                whiteSpace: 'nowrap',
                                alignSelf: 'center',
                            }}>
                                {rec.type === 'warning' ? 'Action needed' :
                                    rec.type === 'info' ? 'Opportunity' : 'Insight'}
                            </span>
                        </div>
                    );
                })}
            </div>

            {/* Under-delivering plans */}
            {underDelivering.length > 0 && (
                <div style={{
                    background: '#141414',
                    border: '0.5px solid #1e1e1e',
                    borderRadius: 12,
                    padding: '14px 16px',
                    marginBottom: 16,
                }}>
                    <div style={{
                        fontSize: 13,
                        fontWeight: 500,
                        color: '#e5e5e5',
                        marginBottom: 12,
                    }}>
                        Under-delivering plans
                    </div>
                    {underDelivering.map(plan => (
                        <div key={plan.id} style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 12,
                            padding: '10px 0',
                            borderBottom: '0.5px solid #1e1e1e',
                            fontSize: 13,
                        }}>
                            <div style={{ flex: 1 }}>
                                <div style={{ fontWeight: 500, color: '#e5e5e5' }}> {plan.name} </div>
                                <div style={{ fontSize: 11, color: '#888', marginTop: 2 }}>
                                    {plan.advertiser} · {plan.flightStart} – {plan.flightEnd}
                                </div>
                            </div>
                            <StatusBadge status={plan.status} />
                            <div style={{ fontSize: 12, color: '#f87171', fontWeight: 500 }}>
                                {plan.completionRate.toFixed(0)}% allocated
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Targeting segments */}
            <div style={{
                background: '#141414',
                border: '0.5px solid #1e1e1e',
                borderRadius: 12,
                padding: '14px 16px',
            }}>
                <div style={{
                    fontSize: 13,
                    fontWeight: 500,
                    color: '#e5e5e5',
                    marginBottom: 12,
                }}>
                    Active targeting segments
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {[
                        { label: 'Adults 25–44', active: true },
                        { label: 'HHI $75K+', active: true },
                        { label: 'Binge watchers', active: true },
                        { label: 'Sports fans', active: false },
                        { label: 'Drama affinity', active: true },
                        { label: 'News viewers', active: false },
                        { label: 'US — Tier 1 cities', active: true },
                        { label: 'Cord cutters', active: true },
                        { label: 'Families', active: false },
                    ].map((seg) => (
                        <span key={seg.label} style={{
                            fontSize: 11,
                            padding: '3px 10px',
                            borderRadius: 99,
                            background: seg.active ? '#1e1b4b' : '#1e1e1e',
                            border: `0.5px solid ${seg.active ? '#4338ca' : '#2a2a2a'}`,
                            color: seg.active ? '#a5b4fc' : '#888',
                        }}>
                            {seg.label}
                        </span>
                    ))}
                </div>
                <div style={{ fontSize: 11, color: '#555', marginTop: 10 }}>
                    Purple = currently in use across active plans
                </div>
            </div>
        </div>
    );
}