import React, { useEffect, useState } from 'react';
import MetricCard from '../components/MetricCard';
import FormField from '../components/FormField';

import {
  createPlacement,
  getPlacements,
  updatePlacement,
  updatePlacementActiveStatus,
} from '../api/placements';

import {
  CreativeType,
  DeviceType,
  Placement,
  PlacementRequest,
  PlacementType,
} from '../types';

import {
  acceptsVideo,
  toggleCreativeType as toggleCreativeTypeInForm,
} from '../utils/placementForm';

// The API client rejects with the backend's error message as a string.
const toErrorMessage = (error: unknown): string =>
  typeof error === 'string' ? error : 'Something went wrong';

const placementTypes: PlacementType[] = [
  'PRE_ROLL',
  'MID_ROLL',
  'POST_ROLL',
  'PAUSE_SCREEN',
  'HOME_HERO',
  'HOME_FEED',
  'SEARCH_RESULTS',
  'CONTENT_DETAILS',
  'LIVE_EVENT_BREAK',
];

const deviceTypes: DeviceType[] = [
  'TV',
  'MOBILE',
  'WEB',
  'TABLET',
];

const creativeTypes: CreativeType[] = [
  'VIDEO',
  'IMAGE',
  'NATIVE',
];

const getInitialForm = (): PlacementRequest => ({
  name: '',
  type: 'PRE_ROLL',
  maxAds: 1,
  maxAdDurationSeconds: 30,
  supportedDevices: ['TV'],
  supportedCreativeTypes: ['VIDEO'],
});

export default function PlacementsPage() {
  const [placements, setPlacements] = useState<Placement[]>([]);
  const [loading, setLoading] = useState(true);

  const [showForm, setShowForm] = useState(false);

  const [editingPlacementId, setEditingPlacementId] =
    useState<number | null>(null);

  const [form, setForm] =
    useState<PlacementRequest>(getInitialForm());

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getPlacements()
      .then(setPlacements)
      .finally(() => setLoading(false));
  }, []);

  const resetForm = () => {
    setForm(getInitialForm());
    setEditingPlacementId(null);
    setShowForm(false);
    setError(null);
  };

  const handleNewPlacement = () => {
    setForm(getInitialForm());
    setEditingPlacementId(null);
    setShowForm(true);
    setError(null);
  };

  const handleEdit = (placement: Placement) => {
    setEditingPlacementId(placement.id);

    setForm({
      name: placement.name,
      type: placement.type,
      maxAds: placement.maxAds,
      maxAdDurationSeconds: placement.maxAdDurationSeconds,
      supportedDevices: [...placement.supportedDevices],
      supportedCreativeTypes: [
        ...placement.supportedCreativeTypes,
      ],
    });

    setShowForm(true);
    setError(null);
  };

  const handleActiveToggle = async (placement: Placement) => {
    const nextActiveState = !placement.active;

    const confirmed = window.confirm(
      nextActiveState
        ? `Activate "${placement.name}"? This placement will become eligible for ad serving.`
        : `Deactivate "${placement.name}"? This placement will no longer be eligible for ad serving.`
    );

    if (!confirmed) {
      return;
    }

    try {
      const updatedPlacement =
        await updatePlacementActiveStatus(
          placement.id,
          nextActiveState
        );

      setPlacements((currentPlacements) =>
        currentPlacements.map((currentPlacement) =>
          currentPlacement.id === updatedPlacement.id
            ? updatedPlacement
            : currentPlacement
        )
      );
    } catch (requestError) {
      setError(toErrorMessage(requestError));
    }
  };

  const handleSubmit = async () => {
    if (
      !form.name.trim() ||
      form.supportedDevices.length === 0 ||
      form.supportedCreativeTypes.length === 0
    ) {
      setError(
        'Name, at least one device and at least one creative type are required'
      );
      return;
    }

    setError(null);

    try {
      if (editingPlacementId !== null) {
        const confirmed = window.confirm(
          `Save changes to "${form.name}"?`
        );

        if (!confirmed) {
          return;
        }

        const updatedPlacement = await updatePlacement(
          editingPlacementId,
          form
        );

        setPlacements((currentPlacements) =>
          currentPlacements.map((placement) =>
            placement.id === updatedPlacement.id
              ? updatedPlacement
              : placement
          )
        );
      } else {
        const newPlacement = await createPlacement(form);

        setPlacements((currentPlacements) => [
          ...currentPlacements,
          newPlacement,
        ]);
      }

      resetForm();
    } catch (requestError) {
      setError(toErrorMessage(requestError));
    }
  };

  const toggleDevice = (device: DeviceType) => {
    setForm((currentForm) => {
      const alreadySelected =
        currentForm.supportedDevices.includes(device);

      return {
        ...currentForm,
        supportedDevices: alreadySelected
          ? currentForm.supportedDevices.filter(
              (currentDevice) =>
                currentDevice !== device
            )
          : [
              ...currentForm.supportedDevices,
              device,
            ],
      };
    });
  };

  const toggleCreativeType = (
    creativeType: CreativeType
  ) => {
    setForm((currentForm) =>
      toggleCreativeTypeInForm(currentForm, creativeType)
    );
  };

  if (loading) {
    return (
      <div
        style={{
          padding: 24,
          color: '#888',
          fontSize: 13,
        }}
      >
        Loading...
      </div>
    );
  }

  const activeCount = placements.filter(
    (placement) => placement.active
  ).length;

  const tvCompatibleCount = placements.filter(
    (placement) =>
      placement.supportedDevices.includes('TV')
  ).length;

  const videoCompatibleCount = placements.filter(
    (placement) =>
      placement.supportedCreativeTypes.includes('VIDEO')
  ).length;

  return (
    <div style={{ padding: 24 }}>

      {/* Top bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 20,
        }}
      >
        <div>
          <h1
            style={{
              fontSize: 18,
              fontWeight: 500,
              color: '#e5e5e5',
              margin: 0,
            }}
          >
            Ad placements
          </h1>

          <div
            style={{
              fontSize: 12,
              color: '#888',
              marginTop: 4,
            }}
          >
            Configure where ads can appear across the
            streaming experience
          </div>
        </div>

        <button
          onClick={
            showForm
              ? resetForm
              : handleNewPlacement
          }
          style={{
            fontSize: 12,
            background: showForm
              ? '#242424'
              : '#E50914',
            color: '#fff',
            border: showForm
              ? '0.5px solid #333'
              : 'none',
            borderRadius: 6,
            padding: '6px 14px',
            fontWeight: 500,
            cursor: 'pointer',
          }}
        >
          {showForm
            ? 'Cancel'
            : '+ New Placement'}
        </button>
      </div>

      {/* Error from the last action */}
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

      {/* Create / Edit form */}
      {showForm && (
        <div
          style={{
            background: '#141414',
            border: '0.5px solid #2a2a2a',
            borderRadius: 12,
            padding: 16,
            marginBottom: 20,
          }}
        >
          <div
            style={{
              fontSize: 13,
              fontWeight: 500,
              color: '#e5e5e5',
              marginBottom: 14,
            }}
          >
            {editingPlacementId !== null
              ? 'Edit placement'
              : 'New placement'}
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns:
                '2fr 1fr 1fr 1fr',
              gap: 10,
              marginBottom: 16,
            }}
          >
            <FormField label="Placement name">
              <input
                placeholder="Placement name"
                value={form.name}
                onChange={(event) =>
                  setForm({
                    ...form,
                    name: event.target.value,
                  })
                }
                style={{
                  fontSize: 12,
                  background: '#1a1a1a',
                  color: '#e5e5e5',
                  border: '0.5px solid #2a2a2a',
                  borderRadius: 6,
                  padding: '7px 10px',
                }}
              />
            </FormField>

            <FormField label="Placement type">
              <select
                value={form.type}
                onChange={(event) =>
                  setForm({
                    ...form,
                    type: event.target
                      .value as PlacementType,
                  })
                }
                style={{
                  fontSize: 12,
                  background: '#1a1a1a',
                  color: '#e5e5e5',
                  border: '0.5px solid #2a2a2a',
                  borderRadius: 6,
                  padding: '7px 10px',
                }}
              >
                {placementTypes.map((type) => (
                  <option
                    key={type}
                    value={type}
                  >
                    {type.replaceAll('_', ' ')}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField label="Max ads" hint="Ads shown per break">
              <input
                type="number"
                min="1"
                placeholder="Max ads"
                value={form.maxAds ?? ''}
                onChange={(event) =>
                  setForm({
                    ...form,
                    maxAds:
                      event.target.value === ''
                        ? null
                        : Number(
                            event.target.value
                          ),
                  })
                }
                style={{
                  fontSize: 12,
                  background: '#1a1a1a',
                  color: '#e5e5e5',
                  border: '0.5px solid #2a2a2a',
                  borderRadius: 6,
                  padding: '7px 10px',
                }}
              />
            </FormField>

            {/* A max ad duration only applies when the placement accepts video */}
            {acceptsVideo(form.supportedCreativeTypes) && (
              <FormField label="Max ad duration (seconds)" hint="Longest video allowed">
                <input
                  type="number"
                  min="1"
                  placeholder="Max ad duration (seconds)"
                  value={form.maxAdDurationSeconds ?? ''}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      maxAdDurationSeconds:
                        event.target.value === ''
                          ? null
                          : Number(
                              event.target.value
                            ),
                    })
                  }
                  style={{
                    fontSize: 12,
                    background: '#1a1a1a',
                    color: '#e5e5e5',
                    border: '0.5px solid #2a2a2a',
                    borderRadius: 6,
                    padding: '7px 10px',
                  }}
                />
              </FormField>
            )}
          </div>

          {/* Supported devices */}
          <div style={{ marginBottom: 14 }}>
            <div
              style={{
                fontSize: 11,
                color: '#888',
                marginBottom: 7,
              }}
            >
              Supported devices
            </div>

            <div
              style={{
                display: 'flex',
                gap: 8,
                flexWrap: 'wrap',
              }}
            >
              {deviceTypes.map((device) => {
                const selected =
                  form.supportedDevices.includes(
                    device
                  );

                return (
                  <button
                    key={device}
                    type="button"
                    onClick={() =>
                      toggleDevice(device)
                    }
                    style={{
                      fontSize: 11,
                      padding: '5px 10px',
                      borderRadius: 6,
                      cursor: 'pointer',
                      border: selected
                        ? '0.5px solid #E50914'
                        : '0.5px solid #2a2a2a',
                      background: selected
                        ? 'rgba(229, 9, 20, 0.12)'
                        : '#1a1a1a',
                      color: selected
                        ? '#fff'
                        : '#888',
                    }}
                  >
                    {device}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Supported creative types */}
          <div style={{ marginBottom: 16 }}>
            <div
              style={{
                fontSize: 11,
                color: '#888',
                marginBottom: 7,
              }}
            >
              Supported creative types
            </div>

            <div
              style={{
                display: 'flex',
                gap: 8,
                flexWrap: 'wrap',
              }}
            >
              {creativeTypes.map(
                (creativeType) => {
                  const selected =
                    form.supportedCreativeTypes.includes(
                      creativeType
                    );

                  return (
                    <button
                      key={creativeType}
                      type="button"
                      onClick={() =>
                        toggleCreativeType(
                          creativeType
                        )
                      }
                      style={{
                        fontSize: 11,
                        padding: '5px 10px',
                        borderRadius: 6,
                        cursor: 'pointer',
                        border: selected
                          ? '0.5px solid #E50914'
                          : '0.5px solid #2a2a2a',
                        background: selected
                          ? 'rgba(229, 9, 20, 0.12)'
                          : '#1a1a1a',
                        color: selected
                          ? '#fff'
                          : '#888',
                      }}
                    >
                      {creativeType}
                    </button>
                  );
                }
              )}
            </div>
          </div>

          {/* Form actions */}
          <div
            style={{
              display: 'flex',
              gap: 8,
            }}
          >
            <button
              onClick={handleSubmit}
              style={{
                fontSize: 12,
                background: '#E50914',
                color: '#fff',
                border: 'none',
                borderRadius: 6,
                padding: '7px 16px',
                cursor: 'pointer',
              }}
            >
              {editingPlacementId !== null
                ? 'Save changes'
                : 'Create placement'}
            </button>

            <button
              onClick={resetForm}
              style={{
                fontSize: 12,
                background: '#242424',
                color: '#aaa',
                border:
                  '0.5px solid #333',
                borderRadius: 6,
                padding: '7px 16px',
                cursor: 'pointer',
              }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Metrics */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns:
            'repeat(4, 1fr)',
          gap: 12,
          marginBottom: 24,
        }}
      >
        <MetricCard
          label="Total placements"
          value={placements.length}
          delta="Configured"
          deltaType="neutral"
        />

        <MetricCard
          label="Active"
          value={activeCount}
          delta="Eligible for serving"
          deltaType="up"
        />

        <MetricCard
          label="TV compatible"
          value={tvCompatibleCount}
          delta="Supports TV"
          deltaType="neutral"
        />

        <MetricCard
          label="Video compatible"
          value={videoCompatibleCount}
          delta="Supports video creatives"
          deltaType="neutral"
        />
      </div>

      {/* Placement configuration */}
      <div
        style={{
          background: '#141414',
          border: '0.5px solid #1e1e1e',
          borderRadius: 12,
          padding: '14px 16px',
        }}
      >
        <div
          style={{
            fontSize: 13,
            fontWeight: 500,
            color: '#e5e5e5',
            marginBottom: 12,
          }}
        >
          Placement configuration
        </div>

        {placements.length === 0 ? (
          <div
            style={{
              color: '#888',
              fontSize: 12,
              padding: '20px 0',
            }}
          >
            No placements configured yet
          </div>
        ) : (
          placements.map((placement) => (
            <div
              key={placement.id}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 16,
                padding: '12px 0',
                borderBottom:
                  '0.5px solid #1e1e1e',
                fontSize: 13,
              }}
            >
              <div style={{ flex: 1 }}>
                <div
                  style={{
                    fontWeight: 500,
                    color: '#e5e5e5',
                  }}
                >
                  {placement.name}
                </div>

                <div
                  style={{
                    fontSize: 11,
                    color: '#888',
                    marginTop: 2,
                  }}
                >
                  {placement.type.replaceAll(
                    '_',
                    ' '
                  )}
                </div>
              </div>

              <div style={{ width: 180 }}>
                <div
                  style={{
                    fontSize: 10,
                    color: '#666',
                    marginBottom: 4,
                  }}
                >
                  DEVICES
                </div>

                <div
                  style={{
                    fontSize: 11,
                    color: '#aaa',
                  }}
                >
                  {placement.supportedDevices.join(
                    ' · '
                  )}
                </div>
              </div>

              <div style={{ width: 140 }}>
                <div
                  style={{
                    fontSize: 10,
                    color: '#666',
                    marginBottom: 4,
                  }}
                >
                  CREATIVES
                </div>

                <div
                  style={{
                    fontSize: 11,
                    color: '#aaa',
                  }}
                >
                  {placement.supportedCreativeTypes.join(
                    ' · '
                  )}
                </div>
              </div>

              <div
                style={{
                  width: 70,
                  fontSize: 12,
                  color: '#888',
                  textAlign: 'right',
                }}
              >
                <div
                  style={{
                    fontSize: 10,
                    color: '#666',
                    marginBottom: 4,
                  }}
                >
                  MAX AD
                </div>
                {placement.maxAdDurationSeconds !==
                null
                  ? `${placement.maxAdDurationSeconds}s`
                  : '—'}
              </div>

              <div
                style={{
                  width: 60,
                  fontSize: 12,
                  color: '#888',
                  textAlign: 'right',
                }}
              >
                <div
                  style={{
                    fontSize: 10,
                    color: '#666',
                    marginBottom: 4,
                  }}
                >
                  MAX ADS
                </div>
                {placement.maxAds !== null
                  ? `${placement.maxAds} ads`
                  : '—'}
              </div>

              <button
                onClick={() =>
                  handleEdit(placement)
                }
                style={{
                  fontSize: 11,
                  color: '#aaa',
                  background: '#1e1e1e',
                  border:
                    '0.5px solid #2a2a2a',
                  borderRadius: 6,
                  padding: '4px 9px',
                  cursor: 'pointer',
                }}
              >
                Edit
              </button>

              <div
                style={{
                  width: 70,
                  textAlign: 'right',
                }}
              >
                <button
                  onClick={() =>
                    handleActiveToggle(
                      placement
                    )
                  }
                  style={{
                    fontSize: 11,
                    padding: '3px 8px',
                    borderRadius: 99,
                    border: 'none',
                    cursor: 'pointer',
                    background:
                      placement.active
                        ? 'rgba(74, 222, 128, 0.1)'
                        : '#1e1e1e',
                    color: placement.active
                      ? '#4ade80'
                      : '#777',
                  }}
                >
                  {placement.active
                    ? 'ACTIVE'
                    : 'INACTIVE'}
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}