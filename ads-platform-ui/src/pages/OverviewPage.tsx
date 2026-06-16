import React, { useEffect, useState } from "react";
import MetricCard from "../components/MetricCard";
import StatusBadge from "../components/StatusBadge";
import { getCampaigns, getActiveCount } from "../api/campaigns";
import { getPendingCount } from "../api/safety";
import { getInventoryStats } from "../api/inventory";
import { Campaign } from "../types";

export default function OverviewPage() {
    const [campaigns, setCampaigns] = useState<Campaign[]>([]);
    const [activeCount, setActiveCount] = useState(0);
    const [pendingFlags, setPendingFlags] = useState(0);
    const [impressions, setImpressions] = useState('0');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            getCampaigns(),
            getActiveCount(),
            getPendingCount(),
            getInventoryStats()
        ]).then(([c, count, flags, stats]) => {
            setCampaigns(c);
            setActiveCount(count);
            setPendingFlags(flags);
            const total = stats.allocatedSlots;
            setImpressions(total >= 1_000_000
                ? `${(total / 1_000_000).toFixed(1)}B`
                : `${(total / 1_000).toFixed(0)}K`);
        }).finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div style={{ padding: 24, color: '#888', fontSize: 13 }}>
                Loading...
            </div>
        );
    }

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
                    Campaign overview
                </h1>
                <select style={{
                    fontSize: 12,
                    background: '#1a1a1a',
                    color: '#e5e5e5',
                    border: '0.5px solid #2a2a2a',
                    borderRadius: 6,
                    padding: '4px 8px',
                }}>
                    <option>Last 30 days</option>
                    <option>Last 7 days</option>
                    <option>This quarter</option>
                </select>
            </div>
            {/* Metric Cards */}
            <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(4, 1fr)',
                gap: 12,
                marginBottom: 24,
            }}>
                <MetricCard
                    label="Active campaigns"
                    value={activeCount}
                    delta="+6 this week"
                    deltaType="up"
                />
                <MetricCard
                    label="Impressions delivered"
                    value={impressions}
                    delta="+8% vs plan"
                    deltaType="up"
                />
                <MetricCard
                    label="Safety flags pending"
                    value={pendingFlags}
                    delta={pendingFlags > 0 ? 'Needs review' : 'All clear'}
                    deltaType={pendingFlags > 0 ? 'down' : 'up'}
                />
                <MetricCard
                    label="Avg completion rate"
                    value="94%"
                    delta="+2pts MoM"
                    deltaType="up"
                />
            </div>
            {/* Campaigns table */}
            <div style={{
                background: '#141414',
                border: '0.5px solid #1e1e1e',
                borderRadius: 12,
                padding: '14px 16px',
            }}>
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: 12,
                }}>
                    <span style={{ fontSize: 13, fontWeight: 500, color: '#e5e5e5' }}>
                        Active Campaigns
                    </span>
                    <span style={{
                        fontSize: 11,
                        padding: '2px 8px',
                        borderRadius: 99,
                        background: '#0c2340',
                        color: '#60a5fa',
                    }}>
                        {campaigns.length} total
                    </span>
                </div>
                {campaigns.length === 0 ? (
                    <div style={{ fontSize: 13, color: '#888', padding: '12px 0' }}>
                        No campaigns yet. Create your first campaign.
                    </div>
                ) : (
                    campaigns.map((c) => (
                        <div key={c.id} style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 12,
                            padding: '8px 0',
                            borderBottom: '0.5px solid #1e1e1e',
                            fontSize: 13,
                        }}>
                            <div style={{ flex: 1 }}>
                                <div style={{ fontWeight: 500, color: '#e5e5e5' }}>{c.name}</div>
                                <div style={{ fontSize: 11, color: '#888', marginTop: 2 }}>{c.agency}</div>
                            </div>
                            <StatusBadge status={c.status} />
                            <div style={{
                                width: 80,
                                height: 5,
                                background: '#2a2a2a',
                                borderRadius: 99,
                                overflow: 'hidden',
                            }}>
                                <div style={{
                                    height: '100%',
                                    borderRadius: 99,
                                    width: `${c.pacingPercent ?? 0}%`,
                                    background:
                                        (c.pacingPercent ?? 0) >= 70 ? '#4ade80' :
                                            (c.pacingPercent ?? 0) >= 40 ? '#fbbf24' : '#f87171',
                                }} />
                            </div>
                            <div style={{ fontSize: 11, color: '#888', width: 36, textAlign: 'right' }}>
                                {c.pacingPercent?.toFixed(0) ?? 0}%
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}