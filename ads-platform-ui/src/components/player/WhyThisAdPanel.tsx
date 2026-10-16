import React from 'react';
import type { AdDecisionResponse } from '../../types/adDecision';

/*
 * Shows the platform's reasoning for one decision:
 * who won and why, and why every other candidate was excluded.
 */
export default function WhyThisAdPanel({
  decision,
}: {
  decision: AdDecisionResponse | null;
}) {
  if (!decision) {
    return (
      <Panel>
        <div style={{ color: '#666', fontSize: 12 }}>
          The decision appears here when the ad break starts.
        </div>
      </Panel>
    );
  }

  const explanation = decision.explanation;

  return (
    <Panel>
      <div style={{ fontSize: 11, color: '#888' }}>Decision</div>
      <div style={{ fontSize: 11, color: '#aaa', fontFamily: 'monospace', marginBottom: 12, wordBreak: 'break-all' }}>
        {decision.decisionId}
      </div>

      {decision.noFillReason && (
        <div style={{ fontSize: 12, color: '#f0a0a0', marginBottom: 12 }}>
          No fill: {decision.noFillReason}
        </div>
      )}

      {explanation && (
        <>
          <div style={{ fontSize: 12, color: '#ccc', marginBottom: 10 }}>
            {explanation.candidateCount} candidate
            {explanation.candidateCount === 1 ? '' : 's'} considered
          </div>

          <SectionTitle>Winners</SectionTitle>
          {explanation.winners.length === 0 ? (
            <Empty>None</Empty>
          ) : (
            explanation.winners.map((winner) => (
              <Row key={winner.slot}>
                <div>
                  Slot {winner.slot}: campaign {winner.campaignId}, creative {winner.creativeId}
                </div>
                <div style={{ color: '#888', fontSize: 11 }}>
                  ${winner.priceCpc.toFixed(2)} × {winner.predictedCtr.toFixed(3)} ={' '}
                  <span style={{ color: '#e5e5e5' }}>{winner.score.toFixed(3)}</span>
                </div>
              </Row>
            ))
          )}

          <SectionTitle>Excluded</SectionTitle>
          {explanation.excluded.length === 0 ? (
            <Empty>None</Empty>
          ) : (
            explanation.excluded.map((exclusion) => (
              <Row key={`${exclusion.campaignId}-${exclusion.creativeId}`}>
                <div>
                  Campaign {exclusion.campaignId}, creative {exclusion.creativeId}
                </div>
                <div style={{ fontSize: 11, color: '#f0a0a0', fontFamily: 'monospace' }}>
                  {exclusion.reasonCode}
                </div>
                <div style={{ fontSize: 11, color: '#888' }}>{exclusion.reason}</div>
              </Row>
            ))
          )}
        </>
      )}
    </Panel>
  );
}

function Panel({ children }: { children: React.ReactNode }) {
  return (
    <div
      style={{
        background: '#141414',
        border: '0.5px solid #2a2a2a',
        borderRadius: 12,
        padding: 16,
        minHeight: 200,
      }}
    >
      <div style={{ fontSize: 13, fontWeight: 500, color: '#e5e5e5', marginBottom: 12 }}>
        Why this ad?
      </div>
      {children}
    </div>
  );
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ fontSize: 10, color: '#666', margin: '12px 0 6px', letterSpacing: 0.5 }}>
      {children}
    </div>
  );
}

function Row({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ fontSize: 12, color: '#ccc', padding: '6px 0', borderBottom: '0.5px solid #1e1e1e' }}>
      {children}
    </div>
  );
}

function Empty({ children }: { children: React.ReactNode }) {
  return <div style={{ fontSize: 12, color: '#666' }}>{children}</div>;
}