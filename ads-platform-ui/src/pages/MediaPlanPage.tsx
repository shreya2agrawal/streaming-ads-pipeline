import React, { useEffect, useState } from 'react';
import MetricCard from '../components/MetricCard';
import StatusBadge from '../components/StatusBadge';
import {
  getAllPlans,
  createPlan,
  sendProposal,
  approvePlan,
  allocateInventory,
  activatePlan,
  getMediaPlanStats,
  cancelPlan,
} from '../api/mediaplan';
import { MediaPlan } from '../types';
import { MediaPlanStats } from '../api/mediaplan';

export default function MediaPlanPage() {
  const [plans, setPlans] = useState<MediaPlan[]>([]);
  const [stats, setStats] = useState<MediaPlanStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [form, setForm] = useState({
    name: '',
    advertiser: '',
    agency: '',
    totalBudget: '',
    flightStart: '',
    flightEnd: '',
    planType: 'DIRECT',
    totalSlotsRequested: '',
    targetShowIds: '',
    targetingSegments: '',
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = () => {
    Promise.all([getAllPlans(), getMediaPlanStats()])
      .then(([p, s]) => {
        setPlans(p);
        setStats(s);
      })
      .finally(() => setLoading(false));
  };

  const handleCreate = () => {
    if (!form.name || !form.advertiser || !form.agency) return;
    createPlan({
      name: form.name,
      advertiser: form.advertiser,
      agency: form.agency,
      totalBudget: parseFloat(form.totalBudget),
      flightStart: form.flightStart,
      flightEnd: form.flightEnd,
      planType: form.planType as any,
      totalSlotsRequested: parseInt(form.totalSlotsRequested),
      targetShowIds: form.targetShowIds.split(',').map(s => s.trim()),
    }).then((newPlan) => {
      setPlans([...plans, newPlan]);
      setShowForm(false);
      setForm({
        name: '', advertiser: '', agency: '', totalBudget: '',
        flightStart: '', flightEnd: '', planType: 'DIRECT',
        totalSlotsRequested: '', targetShowIds: '', targetingSegments: '',
      });
    });
  };

  const handleAction = (id: number, action: string) => {
    setActionLoading(id);
    let call: Promise<MediaPlan>;

    switch (action) {
      case 'send-proposal': call = sendProposal(id); break;
      case 'approve': call = approvePlan(id); break;
      case 'allocate': call = allocateInventory(id); break;
      case 'activate': call = activatePlan(id); break;
      default: return;
    }

    call
      .then((updated) => {
        setPlans(plans.map(p => p.id === id ? updated : p));
      })
      .finally(() => setActionLoading(null));
  };

  const getNextAction = (plan: MediaPlan): { label: string; action: string } | null => {
    switch (plan.status) {
      case 'DRAFT': return { label: 'Send proposal', action: 'send-proposal' };
      case 'PROPOSAL_SENT': return { label: 'Approve', action: 'approve' };
      case 'NEGOTIATING': return { label: 'Approve', action: 'approve' };
      case 'APPROVED':
        if (plan.totalSlotsAllocated === 0) return { label: 'Allocate inventory', action: 'allocate' };
        return { label: 'Activate', action: 'activate' };
      default: return null;
    }
  };

  const handleCancel = (id: number) => {
    if (!window.confirm("Are you sure you want to cancel this plan? Allocated inventory will be released.")) return;
    setActionLoading(id);
    cancelPlan(id)
      .then((updated) => {
        setPlans(plans.map(p => p.id === id ? updated : p));
      })
      .finally(() => setActionLoading(null));
  };

  const pipeline = plans.filter(p =>
    ['DRAFT', 'PROPOSAL_SENT', 'NEGOTIATING'].includes(p.status)
  );

  const active = plans.filter(p => p.status === 'ACTIVE');

  if (loading) return (
    <div style={{ padding: 24, color: '#888', fontSize: 13 }}> Loading... </div>
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
          Media plans
        </h1>
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
          {showForm ? 'Cancel' : '+ New plan'}
        </button>
      </div>
      {/* Create form */}
      {showForm && (
        <div style={{
          background: '#141414',
          border: '0.5px solid #2a2a2a',
          borderRadius: 12,
          padding: 16,
          marginBottom: 20,
        }}>
          <div style={{
            fontSize: 13,
            fontWeight: 500,
            color: '#e5e5e5',
            marginBottom: 12,
          }}>
            New media plan
          </div>
          <div style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr 1fr',
            gap: 10,
          }}>
            {[
              { key: 'name', placeholder: 'Plan name' },
              { key: 'advertiser', placeholder: 'Advertiser' },
              { key: 'agency', placeholder: 'Agency' },
              { key: 'totalBudget', placeholder: 'Total budget (e.g. 4200000)' },
              { key: 'flightStart', placeholder: 'Flight start (YYYY-MM-DD)' },
              { key: 'flightEnd', placeholder: 'Flight end (YYYY-MM-DD)' },
              { key: 'totalSlotsRequested', placeholder: 'Slots requested (e.g. 200000)' },
              { key: 'targetShowIds', placeholder: 'Show IDs (e.g. show-003,show-004)' },
              { key: 'targetingSegments', placeholder: 'Segments (e.g. Adults 25-44,HHI $75K+)' },
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
            <select
              value={form.planType}
              onChange={(e) => setForm({ ...form, planType: e.target.value })}
              style={{
                fontSize: 12,
                background: '#1a1a1a',
                color: '#e5e5e5',
                border: '0.5px solid #2a2a2a',
                borderRadius: 6,
                padding: '6px 10px',
              }}>
              <option value="DIRECT">Direct</option>
              <option value="UPFRONT">Upfront</option>
              <option value="PROGRAMMATIC">Programmatic</option>
              <option value="RFP">RFP</option>
            </select>
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
            Create plan
          </button>
        </div>
      )}
      {/* Metrics */}
      {stats && (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: 12,
          marginBottom: 24,
        }}>
          <MetricCard
            label="Active plans"
            value={stats.activeCount}
            delta="Live campaigns"
            deltaType="up"
          />
          <MetricCard
            label="Pipeline"
            value={pipeline.length}
            delta="In progress"
            deltaType="neutral"
          />
          <MetricCard
            label="Approved"
            value={stats.approvedCount}
            delta="Ready to activate"
            deltaType="up"
          />
          <MetricCard
            label="Completed"
            value={stats.completedCount}
            delta="All time"
            deltaType="neutral"
          />
        </div>
      )}

      {/* Active plans */}
      {active.length > 0 && (
        <div style={{
          background: '#141414',
          border: '0.5px solid #1e1e1e',
          borderRadius: 12,
          padding: '14px 16px',
          marginBottom: 16,
        }}>
          <div style={{
            fontSize: 11,
            fontWeight: 500,
            color: '#555',
            letterSpacing: '0.05em',
            textTransform: 'uppercase',
            marginBottom: 10,
          }}>
            Active plans
          </div>
          {active.map(plan => (
            <PlanRow
              key={plan.id}
              plan={plan}
              actionLoading={actionLoading}
              onAction={handleAction}
              nextAction={getNextAction(plan)}
              onCancel={handleCancel}
            />
          ))}
        </div>
      )}
      {/* Pipeline */}
      <div style={{
        background: '#141414',
        border: '0.5px solid #1e1e1e',
        borderRadius: 12,
        padding: '14px 16px',
        marginBottom: 16,
      }}>
        <div style={{
          fontSize: 11,
          fontWeight: 500,
          color: '#555',
          letterSpacing: '0.05em',
          textTransform: 'uppercase',
          marginBottom: 10,
        }}>
          RFP / Pre-sales pipeline
        </div>
        {pipeline.length === 0 ? (
          <div style={{ fontSize: 13, color: '#888', padding: '8px 0' }}>
            No plans in pipeline.
          </div>
        ) : (
          pipeline.map(plan => (
            <PlanRow
              key={plan.id}
              plan={plan}
              actionLoading={actionLoading}
              onAction={handleAction}
              nextAction={getNextAction(plan)}
              onCancel={handleCancel}
            />
          ))
        )}
      </div>
      {/* Approved — ready to activate */}
      {plans.filter(p => p.status === 'APPROVED').length > 0 && (
        <div style={{
          background: '#141414',
          border: '0.5px solid #1e1e1e',
          borderRadius: 12,
          padding: '14px 16px',
        }}>
          <div style={{
            fontSize: 11,
            fontWeight: 500,
            color: '#555',
            letterSpacing: '0.05em',
            textTransform: 'uppercase',
            marginBottom: 10,
          }}>
            Approved — ready to activate
          </div>
          {plans.filter(p => p.status === 'APPROVED').map(plan => (
            <PlanRow
              key={plan.id}
              plan={plan}
              actionLoading={actionLoading}
              onAction={handleAction}
              nextAction={getNextAction(plan)}
              onCancel={handleCancel}
            />
          ))}
        </div>
      )}

    </div>
  );
}

// Extracted sub-component

interface PlanRowProps {
  plan: MediaPlan;
  actionLoading: number | null;
  onAction: (id: number, action: string) => void;
  nextAction: { label: string; action: string } | null;
  onCancel: (id: number) => void;
}

function PlanRow({ plan, actionLoading, onAction, nextAction, onCancel }: PlanRowProps) {
  return (
    <div style={{
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
          {plan.flightStart} - {plan.flightEnd} ·
          ${(plan.totalBudget / 1_000_000).toFixed(1)}M ·
          {plan.agency}
        </div>
        {plan.completionRate > 0 && (
          <div style={{ fontSize: 11, color: '#555', marginTop: 2 }}>
            Inventory: {plan.completionRate.toFixed(0)}% allocated
          </div>
        )}
      </div>

      <StatusBadge status={plan.planType} />
      <StatusBadge status={plan.status} />

      {nextAction && (
        <button
          onClick={() => onAction(plan.id, nextAction.action)}
          disabled={actionLoading === plan.id}
          style={{
            fontSize: 11,
            background: '#1a1a1a',
            color: '#e5e5e5',
            border: '0.5px solid #2a2a2a',
            borderRadius: 6,
            padding: '4px 12px',
            opacity: actionLoading === plan.id ? 0.5 : 1,
            whiteSpace: 'nowrap',
          }}>
          {actionLoading === plan.id ? 'Loading...' : nextAction.label}
        </button>
      )}
      {!['COMPLETED', 'CANCELLED'].includes(plan.status) && (
        <button
          onClick={() => onCancel(plan.id)}
          disabled={actionLoading === plan.id}
          style={{
            fontSize: 11,
            background: 'transparent',
            color: '#f87171',
            border: '0.5px solid #7f1d1d',
            borderRadius: 6,
            padding: '4px 12px',
            opacity: actionLoading === plan.id ? 0.5 : 1,
            whiteSpace: 'nowrap',
          }}>
          Cancel
        </button>
      )}
    </div>
  );
}