import React, { useState, useEffect } from 'react';
import MetricCard from '../components/MetricCard';
import StatusBadge from '../components/StatusBadge';
import { getSafetyFlags, getPendingCount, reviewFlag } from '../api/safety';
import { SafetyFlag } from '../types';

export default function SafetyPage() {
  const [flags, setFlags] = useState<SafetyFlag[]>([]);
  const [pendingCount, setPendingCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [reviewing, setReviewing] = useState<number | null>(null);

  useEffect(() => {
    Promise.all([getSafetyFlags(), getPendingCount()])
      .then(([f, count]) => {
        setFlags(f);
        setPendingCount(count);
      })
      .finally(() => setLoading(false));
  }, []);

  const handleReview = (id: number, approved: boolean) => {
    setReviewing(id);
    reviewFlag(id, 'reviewer@netflix.com', approved)
      .then((updated) => {
        setFlags(flags.map(f => f.id === id ? updated : f));
        setPendingCount(prev => Math.max(0, prev - 1));
      })
      .finally(() => setReviewing(null));
  };

  const approved = flags.filter(f => f.flagStatus === 'APPROVED').length;
  const rejected = flags.filter(f => f.flagStatus === 'REJECTED').length;
  const pending = flags.filter(f => f.flagStatus === 'PENDING').length;

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
          Brand safety
        </h1>
        {pendingCount > 0 && (
          <span style={{
            fontSize: 12,
            padding: '4px 10px',
            borderRadius: 99,
            background: '#450a0a',
            color: '#f87171',
            fontWeight: 500,
          }}>
            {pendingCount} flags require review
          </span>
        )}
      </div>
      {/* Metrics */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(4, 1fr)',
        gap: 12,
        marginBottom: 24,
      }}>
        <MetricCard
          label="Total flags"
          value={flags.length}
          deltaType="neutral"
        />
        <MetricCard
          label="Pending review"
          value={pending}
          delta={pending > 0 ? 'Needs attention' : 'All clear'}
          deltaType={pending > 0 ? 'down' : 'up'}
        />
        <MetricCard
          label="Approved"
          value={approved}
          delta="Cleared"
          deltaType="up"
        />
        <MetricCard
          label="Rejected"
          value={rejected}
          delta="Blocked"
          deltaType="down"
        />
      </div>
      {/* Flags List */}
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
          All safety flags
        </div>
        {flags.length === 0 ? (
          <div style={{ fontSize: 13, color: '#888', padding: '12px 0' }}>
            No safety flags. All creatives are clear.
          </div>
        ) : (
          flags.map((flag) => (
            <div key={flag.id} style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: 12,
              padding: '10px 0',
              borderBottom: '0.5px solid #1e1e1e',
              fontSize: 13,
            }}>
              {/* Severity icon */}
              <div style={{ fontSize: 16, marginTop: 1 }}>
                {flag.severity === 'POLICY_VIOLATION' ? '🚫' :
                  flag.severity === 'TIER_3' ? '⚠️' : 'ℹ️'}
              </div>
              {/* Details */}
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 500, color: '#e5e5e5' }}>
                  {flag.creativeFile}
                </div>
                <div style={{ fontSize: 11, color: '#888', marginTop: 2 }}>
                  {flag.reason}
                </div>
                {flag.reviewedBy && (
                  <div style={{ fontSize: 11, color: '#555', marginTop: 2 }}>
                    Reviewed by {flag.reviewedBy}
                  </div>
                )}
              </div>

              {/* Severity badge */}
              <StatusBadge status={flag.severity} />

              {/* Status badge */}
              <StatusBadge status={flag.flagStatus} />

              {/* Review buttons - only for pending */}
              {flag.flagStatus === 'PENDING' && (
                <div style={{ display: 'flex', gap: 6 }}>
                  <button
                    onClick={() => handleReview(flag.id, true)}
                    disabled={reviewing === flag.id}
                    style={{
                      fontSize: 11,
                      background: '#14532d',
                      color: '#4ade80',
                      border: '0.5px solid #166534',
                      borderRadius: 6,
                      padding: '3px 10px',
                      opacity: reviewing === flag.id ? 0.5 : 1,
                    }}>
                    Approve
                  </button>
                  <button
                    onClick={() => handleReview(flag.id, false)}
                    disabled={reviewing === flag.id}
                    style={{
                      fontSize: 11,
                      background: '#450a0a',
                      color: '#f87171',
                      border: '0.5px solid #7f1d1d',
                      borderRadius: 6,
                      padding: '3px 10px',
                      opacity: reviewing === flag.id ? 0.5 : 1,
                    }}>
                    Reject
                  </button>
                </div>
              )}
            </div>
          ))
        )}
      </div>
      {/* GARM rules reference */}
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
          GARM category rules
        </div>
        {[
          { rule: 'Adult content', action: 'Block all ad serving', status: 'Enforced' },
          { rule: 'Violence (Tier 1-2)', action: 'Notify advertiser', status: 'Enforced' },
          { rule: 'Violence (Tier 3)', action: 'Human review required', status: 'Enforced' },
          { rule: 'Policy violation', action: 'Immediate block', status: 'Enforced' },
          { rule: 'Misinformation', action: 'ML classifier active', status: 'Enforced' },
        ].map((item, i) => (
          <div key={i} style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: '7px 0',
            borderBottom: i < 4 ? '0.5px solid #1e1e1e' : 'none',
            fontSize: 12,
          }}>
            <span style={{ fontSize: 14 }}>✅</span>
            <div style={{ flex: 1, color: '#e5e5e5' }}>{item.rule} — {item.action}</div>
            <span style={{
              fontSize: 11,
              padding: '2px 8px',
              borderRadius: 99,
              background: '#14532d',
              color: '#4ade80',
            }}>
              {item.status}
            </span>
          </div>
        ))}
      </div>
      
    </div>
  );
}