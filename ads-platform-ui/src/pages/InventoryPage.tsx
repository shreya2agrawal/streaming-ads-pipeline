import React, { useEffect, useState } from 'react';
import MetricCard from '../components/MetricCard';
import StatusBadge from '../components/StatusBadge';
import { getAllInventory, getInventoryStats } from '../api/inventory';
import { Inventory, InventoryStats } from '../types';

export default function InventoryPage() {
  const [inventory, setInventory] = useState<Inventory[]>([]);
  const [stats, setStats] = useState<InventoryStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getAllInventory(), getInventoryStats()])
      .then(([inv, s]) => {
        setInventory(inv);
        setStats(s);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return (
    <div style={{ padding: 24, color: '#888', fontSize: 13 }}>Loading...</div>
  );

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
          Inventory allocation
        </h1>
        <select style={{
          fontSize: 12,
          background: '#1a1a1a',
          color: '#e5e5e5',
          border: '0.5px solid #2a2a2a',
          borderRadius: 6,
          padding: '4px 8px',
        }}>
          <option>All genres</option>
          <option>Drama</option>
          <option>Thriller</option>
          <option>Comedy</option>
        </select>
      </div>

      {/* Metrics */}
      {stats && (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: 12,
          marginBottom: 24,
        }}>
          <MetricCard
            label="Total inventory"
            value={`${(stats.totalSlots / 1_000_000).toFixed(1)}M`}
            delta="slots / month"
            deltaType="neutral"
          />
          <MetricCard
            label="Allocated"
            value={`${(stats.allocatedSlots / 1_000_000).toFixed(1)}M`}
            delta={`${stats.overallFillRate.toFixed(0)}% fill rate`}
            deltaType="up"
          />
          <MetricCard
            label="Available"
            value={`${(stats.availableSlots / 1_000).toFixed(0)}K`}
            delta="Open for booking"
            deltaType="up"
          />
          <MetricCard
            label="Shows tracked"
            value={inventory.length}
            delta="All titles"
            deltaType="neutral"
          />
        </div>
      )}

      {/* Inventory table */}
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
          Inventory by title
        </div>

        {inventory.map((item) => (
          <div key={item.id} style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: '10px 0',
            borderBottom: '0.5px solid #1e1e1e',
            fontSize: 13,
          }}>
            {/* Title info */}
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 500, color: '#e5e5e5' }}>{item.title}</div>
              <div style={{ fontSize: 11, color: '#888', marginTop: 2 }}>
                {item.genre} · {item.contentRating}
              </div>
            </div>

            {/* Fill rate bar */}
            <div style={{ width: 120 }}>
              <div style={{
                width: '100%',
                height: 5,
                background: '#2a2a2a',
                borderRadius: 99,
                overflow: 'hidden',
              }}>
                <div style={{
                  height: '100%',
                  borderRadius: 99,
                  width: `${Math.min(item.fillRate, 100)}%`,
                  background: '#7c3aed',
                }} />
              </div>
            </div>

            {/* Available slots */}
            <div style={{
              fontSize: 12,
              color: item.fillRate >= 95 ? '#f87171' :
                     item.fillRate >= 75 ? '#fbbf24' : '#888',
              width: 80,
              textAlign: 'right',
            }}>
              {(item.availableSlots / 1_000).toFixed(0)}K avail
            </div>

            {/* Fill rate % */}
            <div style={{
              fontSize: 12,
              color: '#888',
              width: 40,
              textAlign: 'right',
            }}>
              {item.fillRate.toFixed(0)}%
            </div>

            {/* Status badge */}
            <StatusBadge status={item.status} />
          </div>
        ))}
      </div>

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
          Available targeting segments
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {[
            'Adults 25–44', 'HHI $75K+', 'Binge watchers',
            'Sports fans', 'Drama affinity', 'News viewers',
            'US — Tier 1 cities', 'Spanish language',
            'Cord cutters', 'Families',
          ].map((seg) => (
            <span key={seg} style={{
              fontSize: 11,
              padding: '3px 10px',
              borderRadius: 99,
              background: '#1e1e1e',
              border: '0.5px solid #2a2a2a',
              color: '#888',
            }}>
              {seg}
            </span>
          ))}
        </div>
      </div>

    </div>
  );
}