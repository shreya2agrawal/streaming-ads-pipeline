import React from "react";

interface Props {
    label: string;
    value: string | number;
    delta?: string;
    deltaType?: 'up' | 'down' | 'neutral';
}

export default function MetricCard({ label, value, delta, deltaType = 'neutral' }: Props) {
    const deltaColor =
        deltaType === 'up' ? '#4ade80' :
            deltaType === 'down' ? '#f87171' :
                '#888888';

    return (
        <div style={{
            background: '#141414',
            border: '0.5px solid #1e1e1e',
            borderRadius: 8,
            padding: '12px 16px',
        }}>
            <div style={{
                fontSize: 11,
                color: '#888',
                marginBottom: 4,
            }}>
                {label}
            </div>
            <div style={{
                fontSize: 24,
                fontWeight: 500,
                color: '#e5e5e5',
            }}>
                {value}
            </div>
            {delta && (
                <div style={{
                    fontSize: 11,
                    color: deltaColor,
                    marginTop: 3,
                }}>
                    {delta}
                </div>
            )}
        </div>
    );
}