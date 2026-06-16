import React from "react";

type Status =
    | 'ACTIVE' | 'PACING_LOW' | 'AT_RISK' | 'DRAFT' | 'PAUSED' | 'COMPLETED'
    | 'PENDING' | 'APPROVED' | 'REJECTED'
    | 'ADVISORY' | 'TIER_3' | 'POLICY_VIOLATION'
    | 'AVAILABLE' | 'NEAR_SOLD_OUT' | 'SOLD_OUT'
    | 'PROPOSAL_SENT' | 'NEGOTIATING' | 'CANCELLED'
    | 'UPFRONT' | 'PROGRAMMATIC' | 'DIRECT' | 'RFP';

interface Config {
    label: string;
    bg: string;
    color: string;
}

const configs: Record<Status, Config> = {
    ACTIVE: { label: 'Active', bg: '#14532d', color: '#4ade80' },
    PACING_LOW: { label: 'Pacing low', bg: '#451a03', color: '#fbbf24' },
    AT_RISK: { label: 'At risk', bg: '#450a0a', color: '#f87171' },
    DRAFT: { label: 'Draft', bg: '#1e1e1e', color: '#888888' },
    PAUSED: { label: 'Paused', bg: '#1e1e1e', color: '#888888' },
    COMPLETED: { label: 'Completed', bg: '#0c2340', color: '#60a5fa' },
    PENDING: { label: 'Pending review', bg: '#451a03', color: '#fbbf24' },
    APPROVED: { label: 'Approved', bg: '#14532d', color: '#4ade80' },
    REJECTED: { label: 'Rejected', bg: '#450a0a', color: '#f87171' },
    ADVISORY: { label: 'Advisory', bg: '#0c2340', color: '#60a5fa' },
    TIER_3: { label: 'Tier 3', bg: '#451a03', color: '#fbbf24' },
    POLICY_VIOLATION: { label: 'Policy violation', bg: '#450a0a', color: '#f87171' },
    AVAILABLE: { label: 'Available', bg: '#14532d', color: '#4ade80' },
    NEAR_SOLD_OUT: { label: 'Near sold out', bg: '#451a03', color: '#fbbf24' },
    SOLD_OUT: { label: 'Sold out', bg: '#450a0a', color: '#f87171' },
    PROPOSAL_SENT: { label: 'Proposal sent', bg: '#0c2340', color: '#60a5fa' },
    NEGOTIATING: { label: 'Negotiating', bg: '#1e1e1e', color: '#888888' },
    CANCELLED: { label: 'Cancelled', bg: '#450a0a', color: '#f87171' },
    UPFRONT: { label: 'Upfront', bg: '#1e1e1e', color: '#888888' },
    PROGRAMMATIC: { label: 'Programmatic', bg: '#1e1e1e', color: '#888888' },
    DIRECT: { label: 'Direct', bg: '#0c2340', color: '#60a5fa' },
    RFP: { label: 'RFP', bg: '#1a1b4b', color: '#a5b4fc' },
};

interface Props {
    status: Status;
}

export default function StatusBadge({ status }: Props) {

    const config = configs[status] ?? {
        label: status,
        bg: '#1e1e1e',
        color: '#888888'
    };

    return (
        <span style={{
            fontSize: 11,
            fontWeight: 500,
            padding: '2px 8px',
            borderRadius: 99,
            backgroundColor: config.bg,
            color: config.color,
            whiteSpace: 'nowrap',
        }}>
            {config.label}
        </span>
    );
}
