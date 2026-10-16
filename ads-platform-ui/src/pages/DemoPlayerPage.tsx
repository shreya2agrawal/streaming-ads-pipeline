import React, { useCallback, useEffect, useState } from 'react';
import { getActivePlacements } from '../api/placements';
import { requestAdDecision } from '../api/adDecisions';
import type { DeviceType, Placement } from '../types';
import type { AdDecisionResponse } from '../types/adDecision';
import PlayerScreen, { PlayerPhase } from '../components/player/PlayerScreen';
import WhyThisAdPanel from '../components/player/WhyThisAdPanel';

// Seconds of "show" before the ad break.
const BREAK_AT_SECONDS = 6;

// Flip to true once the click endpoint exists.
const CLICK_TRACKING_READY = true;

const NO_FILL_PAUSE_MS = 2500;

// The API client rejects with the backend's error message as a string.
const toErrorMessage = (error: unknown): string =>
  typeof error === 'string' ? error : 'Something went wrong';

/*
 * A simulated streaming app. The player is simulated, but the
 * request it sends is exactly what a real TV app would send.
 */
export default function DemoPlayerPage() {
  const [placements, setPlacements] = useState<Placement[]>([]);
  const [placementId, setPlacementId] = useState<number | null>(null);
  const [deviceType, setDeviceType] = useState<DeviceType>('TV');
  const [showId, setShowId] = useState('show-711');
  const [segments, setSegments] = useState('HOME_IMPROVEMENT');
  const [slots, setSlots] = useState(2);

  const [phase, setPhase] = useState<PlayerPhase>('idle');
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [decision, setDecision] = useState<AdDecisionResponse | null>(null);
  const [adIndex, setAdIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getActivePlacements()
      .then((list) => {
        setPlacements(list);
        if (list.length > 0) {
          setPlacementId(list[0].id);
          setDeviceType(list[0].supportedDevices[0]);
        }
      })
      .catch((requestError) => setError(toErrorMessage(requestError)));
  }, []);

  const selectedPlacement =
    placements.find((placement) => placement.id === placementId) ?? null;

  // The "show" advances while playing.
  useEffect(() => {
    if (phase !== 'playing') {
      return;
    }
    const timer = window.setInterval(
      () => setElapsedSeconds((seconds) => seconds + 0.1),
      100
    );
    return () => window.clearInterval(timer);
  }, [phase]);

  // At the break, ask the platform for ads, exactly as a TV app would.
  useEffect(() => {
    if (phase !== 'playing' || elapsedSeconds < BREAK_AT_SECONDS || placementId === null) {
      return;
    }

    setPhase('requesting');

    requestAdDecision(
      {
        placementId,
        deviceType,
        showId,
        viewerTrackId: 'demo-viewer',
        audienceSegments: segments
          .split(',')
          .map((segment) => segment.trim())
          .filter((segment) => segment.length > 0),
        availableSlots: slots,
        viewerTimeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      },
      true
    )
      .then((response) => {
        setDecision(response);
        setAdIndex(0);
        setPhase(response.ads.length > 0 ? 'ad' : 'noFill');
      })
      .catch((requestError) => {
        setError(toErrorMessage(requestError));
        setPhase('idle');
      });
  }, [phase, elapsedSeconds, placementId, deviceType, showId, segments, slots]);

  // A no-fill pauses briefly, then the show continues.
  useEffect(() => {
    if (phase !== 'noFill') {
      return;
    }
    const timer = window.setTimeout(() => setPhase('resumed'), NO_FILL_PAUSE_MS);
    return () => window.clearTimeout(timer);
  }, [phase]);

  const handleAdFinished = useCallback(() => {
    const adCount = decision?.ads.length ?? 0;
    if (adIndex + 1 < adCount) {
      setAdIndex(adIndex + 1);
    } else {
      setPhase('resumed');
    }
  }, [adIndex, decision]);

  const handlePlay = () => {
    setError(null);
    setDecision(null);
    setAdIndex(0);
    setElapsedSeconds(0);
    setPhase('playing');
  };

  const busy = phase === 'playing' || phase === 'requesting' || phase === 'ad' || phase === 'noFill';
  const currentAd = decision?.ads[adIndex] ?? null;

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 18, fontWeight: 500, color: '#e5e5e5', margin: 0 }}>
        Demo player
      </h1>
      <div style={{ fontSize: 12, color: '#888', marginTop: 4, marginBottom: 20 }}>
        A simulated streaming app. At the ad break it calls the real decision API.
      </div>

      {error && (
        <div
          role="alert"
          style={{
            fontSize: 12,
            color: '#f0a0a0',
            background: '#2a1515',
            border: '0.5px solid #5a2a2a',
            borderRadius: 6,
            padding: '8px 12px',
            marginBottom: 16,
          }}
        >
          {error}
        </div>
      )}

      {/* Controls */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 12,
          alignItems: 'flex-end',
          background: '#141414',
          border: '0.5px solid #2a2a2a',
          borderRadius: 12,
          padding: 16,
          marginBottom: 16,
        }}
      >
        <Field label="Placement">
          <select
            value={placementId ?? ''}
            disabled={busy}
            onChange={(event) => {
              const id = Number(event.target.value);
              setPlacementId(id);
              const placement = placements.find((p) => p.id === id);
              if (placement) {
                setDeviceType(placement.supportedDevices[0]);
              }
            }}
            style={inputStyle}
          >
            {placements.map((placement) => (
              <option key={placement.id} value={placement.id}>
                {placement.name}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Device">
          <select
            value={deviceType}
            disabled={busy}
            onChange={(event) => setDeviceType(event.target.value as DeviceType)}
            style={inputStyle}
          >
            {(selectedPlacement?.supportedDevices ?? []).map((device) => (
              <option key={device} value={device}>
                {device}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Show">
          <input
            value={showId}
            disabled={busy}
            onChange={(event) => setShowId(event.target.value)}
            style={inputStyle}
          />
        </Field>

        <Field label="Viewer segments (comma separated)">
          <input
            value={segments}
            disabled={busy}
            onChange={(event) => setSegments(event.target.value)}
            style={{ ...inputStyle, minWidth: 220 }}
          />
        </Field>

        <Field label="Slots">
          <input
            type="number"
            min={1}
            max={10}
            value={slots}
            disabled={busy}
            onChange={(event) => setSlots(Math.max(1, Number(event.target.value)))}
            style={{ ...inputStyle, width: 70 }}
          />
        </Field>

        <button
          onClick={handlePlay}
          disabled={busy || placementId === null}
          style={{
            fontSize: 12,
            background: busy ? '#242424' : '#E50914',
            color: '#fff',
            border: 'none',
            borderRadius: 6,
            padding: '8px 18px',
            cursor: busy ? 'not-allowed' : 'pointer',
          }}
        >
          {phase === 'resumed' ? '↻ Play again' : '▶ Play'}
        </button>
      </div>

      {placements.length === 0 && !error && (
        <div style={{ fontSize: 12, color: '#888', marginBottom: 16 }}>
          No active placements. Activate one on the Placements page first.
        </div>
      )}

      {/* Screen + explanation */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 3fr) minmax(0, 2fr)', gap: 16 }}>
        <PlayerScreen
          phase={phase}
          showId={showId}
          elapsedSeconds={elapsedSeconds}
          breakAtSeconds={BREAK_AT_SECONDS}
          ad={currentAd}
          adNumber={adIndex + 1}
          adCount={decision?.ads.length ?? 0}
          clickTrackingReady={CLICK_TRACKING_READY}
          onAdFinished={handleAdFinished}
        />
        <WhyThisAdPanel decision={decision} />
      </div>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  fontSize: 12,
  background: '#1a1a1a',
  color: '#e5e5e5',
  border: '0.5px solid #2a2a2a',
  borderRadius: 6,
  padding: '7px 10px',
};

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
      <span style={{ fontSize: 11, color: '#888' }}>{label}</span>
      {children}
    </label>
  );
}