import React, { useEffect, useState } from 'react';
import MetricCard from '../components/MetricCard';
import StatusBadge from '../components/StatusBadge';
import { getCampaigns, createCampaign } from '../api/campaigns';
import { Campaign } from '../types';

export default function CampaignsPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    name: '',
    advertiser: '',
    agency: '',
    budget: '',
    flightStart: '',
    flightEnd: '',
    impressionsGoal: ''
  });

  useEffect(() => {
    getCampaigns()
      .then(setCampaigns)
      .finally(() => setLoading(false));
  }, []);

  const handleCreate = () => {
    if (!form.name || !form.advertiser || !form.agency) return;
    createCampaign({
      name: form.name,
      advertiser: form.advertiser,
      agency: form.agency,
      budget: parseFloat(form.budget),
      flightStart: form.flightStart,
      flightEnd: form.flightEnd,
      impressionsGoal: parseInt(form.impressionsGoal),
    }).then((newCampaign) => {
      setCampaigns([...campaigns, newCampaign]);
      setShowForm(false);
      setForm({ name: '', advertiser: '', agency: '', budget: '', flightStart: '', flightEnd: '', impressionsGoal: '' });
    });
  };

  const atRisk = campaigns.filter(c => c.status === 'AT_RISK').length;
  const completed = campaigns.filter(c => c.status === 'COMPLETED').length;
  const totalBudget = campaigns.reduce((sum, c) => sum + (c.budget || 0), 0);

  if (loading) return <div style={{ padding: 24, color: '#888', fontSize: 13 }}>
    Loading...
  </div>;

  return (
    <div style={{ padding: 24 }}>
      {/* Top bar */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 20,
      }}>
        <h1 style={{ fontSize: 18, fontWeight: 500, color: '#e5e5e5' }}>Campaigns</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          style={{
            fontSize: 12,
            background: '#E50914',
            color: '#fff',
            border: 'none',
            borderRadius: 6,
            padding: '6px 14px',
            fontWeight: 500,
          }}>
          {showForm ? 'Cancel' : '+ New Campaign'}
        </button>
      </div>

      {/* Create Form */}
      {showForm && (
        <div style={{
          background: '#141414',
          border: '0.5px solid #2a2a2a',
          borderRadius: 12,
          padding: 16,
          marginBottom: 20,
        }}>
          <div style={{ fontSize: 13, fontWeight: 500, color: '#e5e5e5', marginBottom: 12 }}>
            New campaign
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
            {[
              { key: 'name', placeholder: 'Campaign name' },
              { key: 'advertiser', placeholder: 'Advertiser' },
              { key: 'agency', placeholder: 'Agency' },
              { key: 'budget', placeholder: 'Budget (e.g. 4200000)' },
              { key: 'flightStart', placeholder: 'Flight start (YYYY-MM-DD)' },
              { key: 'flightEnd', placeholder: 'Flight end (YYYY-MM-DD)' },
              { key: 'impressionsGoal', placeholder: 'Impressions goal' },
            ].map(({ key, placeholder }) => (
              <input
                key={key}
                placeholder={placeholder}
                value={(form as any)[key]}
                onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                style={{
                  fontSize: 12,
                  background: '#1a1a1a',
                  color: '#e5e5e5',
                  border: '0.5px solid #2a2a2a',
                  borderRadius: 6,
                  padding: '6px 10px',
                }}
              />
            ))}
          </div>
          <button
            onClick={handleCreate}
            style={{
              marginTop: 12,
              fontSize: 12,
              background: '#E50914',
              color: '#fff',
              border: 'none',
              borderRadius: 6,
              padding: '6px 16px',
            }}>
            Create campaign
          </button>
        </div>
      )}
      {/* Metrics */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(4, 1fr)',
        gap: 12,
        marginBottom: 24,
      }}>
        <MetricCard label="Total campaigns" value={campaigns.length} delta="All time" deltaType="neutral" />
        <MetricCard
          label="Total budget"
          value={`$${(totalBudget / 1_000_000).toFixed(1)}M`}
          delta="Combined"
          deltaType="neutral"
        />
        <MetricCard label="At risk" value={atRisk} delta={atRisk > 0 ? 'Needs attention' : 'All good'} deltaType={atRisk > 0 ? 'down' : 'up'} />
        <MetricCard label="Completed" value={completed} deltaType="neutral" />
      </div>
      {/* Campaigns List */}
      <div style={{
        background: '#141414',
        border: '0.5px solid #1e1e1e',
        borderRadius: 12,
        padding: '14px 16px',
      }}>
        <div style={{ fontSize: 13, fontWeight: 500, color: '#e5e5e5', marginBottom: 12 }}>
          All campaigns
        </div>
        {campaigns.length === 0 ? (
          <div style={{ fontSize: 13, color: '#888', padding: '12px 0' }}>
            No campaigns yet. Create your first one above.
          </div>
        ) : (
          campaigns.map((c) => (
            <div key={c.id} style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              padding: '10px 0',
              borderBottom: '0.5px solid #1e1e1e',
              fontSize: 13,
            }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 500, color: '#e5e5e5' }}>{c.name}</div>
                <div style={{ fontSize: 11, color: '#888', marginTop: 2 }}>
                  {c.agency} · ${(c.budget / 1_000_000).toFixed(1)}M · {c.flightStart} – {c.flightEnd}
                </div>
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