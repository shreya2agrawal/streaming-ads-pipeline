import React, { useEffect, useState } from 'react';
import { toAbsoluteClickUrl } from '../../api/adDecisions';
import type { ServedAd } from '../../types/adDecision';

export type PlayerPhase =
  | 'idle'
  | 'playing'
  | 'requesting'
  | 'ad'
  | 'noFill'
  | 'resumed';

// Images and native ads have no running time of their own.
const STATIC_AD_SECONDS = 8;

interface PlayerScreenProps {
  phase: PlayerPhase;
  showId: string;
  elapsedSeconds: number;
  breakAtSeconds: number;
  ad: ServedAd | null;
  adNumber: number;
  adCount: number;
  clickTrackingReady: boolean;
  onAdFinished: () => void;
}

/*
 * Plays the role of a streaming app's player: a show runs,
 * reaches an ad break, shows the ads the platform chose,
 * then returns to the show.
 */
export default function PlayerScreen(props: PlayerScreenProps) {
  const { phase, showId, elapsedSeconds, breakAtSeconds, ad } = props;

  return (
    <div
      style={{
        position: 'relative',
        aspectRatio: '16 / 9',
        background: '#000',
        border: '0.5px solid #2a2a2a',
        borderRadius: 12,
        overflow: 'hidden',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {phase === 'idle' && (
        <div style={{ color: '#666', fontSize: 13 }}>
          Press Play to start the show
        </div>
      )}

      {(phase === 'playing' || phase === 'requesting') && (
        <ShowView
          showId={showId}
          elapsedSeconds={elapsedSeconds}
          breakAtSeconds={breakAtSeconds}
          message={phase === 'requesting' ? 'Ad break: asking the platform for ads...' : null}
        />
      )}

      {phase === 'ad' && ad && (
        // key resets the countdown for each new ad
        <AdView
          key={ad.slot}
          ad={ad}
          adNumber={props.adNumber}
          adCount={props.adCount}
          clickTrackingReady={props.clickTrackingReady}
          onFinished={props.onAdFinished}
        />
      )}

      {phase === 'noFill' && (
        <div style={{ color: '#aaa', fontSize: 13, textAlign: 'center' }}>
          No ad for this break
          <div style={{ color: '#666', fontSize: 11, marginTop: 6 }}>
            Returning to the show
          </div>
        </div>
      )}

      {phase === 'resumed' && (
        <ShowView
          showId={showId}
          elapsedSeconds={breakAtSeconds}
          breakAtSeconds={breakAtSeconds}
          message="Back to the show"
        />
      )}
    </div>
  );
}

function ShowView({
  showId,
  elapsedSeconds,
  breakAtSeconds,
  message,
}: {
  showId: string;
  elapsedSeconds: number;
  breakAtSeconds: number;
  message: string | null;
}) {
  const progress = Math.min(elapsedSeconds / breakAtSeconds, 1) * 100;

  return (
    <div style={{ width: '100%', height: '100%', padding: 24, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
      <div style={{ fontSize: 13, color: '#ccc' }}>
        Now playing: <span style={{ color: '#fff', fontWeight: 500 }}>{showId}</span>
      </div>

      {message && (
        <div style={{ alignSelf: 'center', fontSize: 13, color: '#e5e5e5' }}>
          {message}
        </div>
      )}

      <div>
        <div style={{ height: 4, background: '#333', borderRadius: 2, overflow: 'hidden' }}>
          <div style={{ width: `${progress}%`, height: '100%', background: '#E50914' }} />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#888', marginTop: 6 }}>
          <span>0:{String(Math.floor(elapsedSeconds)).padStart(2, '0')}</span>
          <span>Ad break at 0:{String(breakAtSeconds).padStart(2, '0')}</span>
        </div>
      </div>
    </div>
  );
}

function AdView({
  ad,
  adNumber,
  adCount,
  clickTrackingReady,
  onFinished,
}: {
  ad: ServedAd;
  adNumber: number;
  adCount: number;
  clickTrackingReady: boolean;
  onFinished: () => void;
}) {
  const totalSeconds = ad.durationSeconds ?? STATIC_AD_SECONDS;
  const [secondsLeft, setSecondsLeft] = useState(totalSeconds);
  const [mediaFailed, setMediaFailed] = useState(false);

  // The countdown drives the break, so the demo works even if media fails.
  useEffect(() => {
    if (secondsLeft <= 0) {
      onFinished();
      return;
    }
    const timer = window.setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [secondsLeft, onFinished]);

  // The click goes through the platform's tracking URL, which records
  // and charges the click, then redirects to the advertiser.
  const handleLearnMore = () => {
    window.open(toAbsoluteClickUrl(ad.clickUrl), '_blank', 'noopener,noreferrer');
  };

  return (
    <div style={{ position: 'absolute', inset: 0 }}>
      {!mediaFailed && ad.creativeType === 'VIDEO' && (
        <video
          src={ad.mediaUrl}
          autoPlay
          muted
          playsInline
          onError={() => setMediaFailed(true)}
          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        />
      )}

      {!mediaFailed && ad.creativeType !== 'VIDEO' && (
        <img
          src={ad.mediaUrl}
          alt="Advertisement"
          onError={() => setMediaFailed(true)}
          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        />
      )}

      {mediaFailed && (
        <div
          style={{
            width: '100%',
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'linear-gradient(135deg, #1d3b4f, #3c6e71)',
            color: '#fff',
            gap: 6,
          }}
        >
          <div style={{ fontSize: 16, fontWeight: 600 }}>
            Campaign {ad.campaignId}, creative {ad.creativeId}
          </div>
          <div style={{ fontSize: 11, opacity: 0.75 }}>
            {ad.creativeType} ad. Media could not load: {ad.mediaUrl}
          </div>
        </div>
      )}

      <div style={{ position: 'absolute', top: 12, left: 12, fontSize: 11, background: 'rgba(0,0,0,0.6)', color: '#fff', padding: '3px 8px', borderRadius: 4 }}>
        Sponsored · Ad {adNumber} of {adCount}
      </div>

      <div style={{ position: 'absolute', top: 12, right: 12, fontSize: 11, background: 'rgba(0,0,0,0.6)', color: '#fff', padding: '3px 8px', borderRadius: 4 }}>
        {secondsLeft}s
      </div>

      <button
        onClick={handleLearnMore}
        disabled={!clickTrackingReady}
        title={clickTrackingReady ? undefined : 'Click tracking is the next step'}
        style={{
          position: 'absolute',
          bottom: 16,
          right: 16,
          fontSize: 12,
          padding: '7px 14px',
          borderRadius: 6,
          border: 'none',
          background: clickTrackingReady ? '#fff' : 'rgba(255,255,255,0.35)',
          color: '#000',
          cursor: clickTrackingReady ? 'pointer' : 'not-allowed',
        }}
      >
        Learn more
      </button>
    </div>
  );
}