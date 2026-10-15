import React, { useEffect, useRef, useState } from "react";

import MetricCard from "../components/MetricCard";
import StatusBadge from "../components/StatusBadge";

import {
  createCampaign,
  deleteCampaign,
  getCampaigns,
  updateCampaign,
} from "../api/campaigns";

import { createPlacement, getPlacements } from "../api/placements";

import {
  Campaign,
  CreativeType,
  DeviceType,
  Placement,
  PlacementRequest,
  PlacementType,
} from "../types";

import {
  acceptsVideo,
  toggleCreativeType as toggleCreativeTypeInForm,
} from "../utils/placementForm";

// The API client rejects with the backend's error message as a string.
const toErrorMessage = (error: unknown): string =>
  typeof error === "string" ? error : "Something went wrong";

interface CampaignFormState {
  name: string;
  advertiser: string;
  agency: string;
  budget: string;
  flightStart: string;
  flightEnd: string;
  impressionsGoal: string;
  targetShowIds: string;
  targetingSegments: string;
  targetPlacementIds: number[];
  targetDeviceTypes: DeviceType[];
}

const createEmptyCampaignForm = (): CampaignFormState => ({
  name: "",
  advertiser: "",
  agency: "",
  budget: "",
  flightStart: "",
  flightEnd: "",
  impressionsGoal: "",
  targetShowIds: "",
  targetingSegments: "",
  targetPlacementIds: [],
  targetDeviceTypes: [],
});

const createEmptyPlacementForm = (): PlacementRequest => ({
  name: "",
  type: "PRE_ROLL",
  maxAds: 1,
  maxAdDurationSeconds: 30,
  supportedDevices: ["TV"],
  supportedCreativeTypes: ["VIDEO"],
});

const deviceTypes: DeviceType[] = [
  "TV",
  "MOBILE",
  "WEB",
  "TABLET",
];

const creativeTypes: CreativeType[] = [
  "VIDEO",
  "IMAGE",
  "NATIVE",
];

const placementTypes: PlacementType[] = [
  "PRE_ROLL",
  "MID_ROLL",
  "POST_ROLL",
  "PAUSE_SCREEN",
  "HOME_HERO",
  "HOME_FEED",
  "SEARCH_RESULTS",
  "CONTENT_DETAILS",
  "LIVE_EVENT_BREAK",
];

export default function CampaignsPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);

  const [placements, setPlacements] = useState<Placement[]>([]);

  const [loading, setLoading] = useState(true);

  const [
    showCampaignForm,
    setShowCampaignForm,
  ] = useState(false);

  const [
    editingCampaignId,
    setEditingCampaignId,
  ] = useState<number | null>(null);

  const [
    showPlacementModal,
    setShowPlacementModal,
  ] = useState(false);

  const [
    creatingPlacement,
    setCreatingPlacement,
  ] = useState(false);

  const [
    placementError,
    setPlacementError,
  ] = useState<string | null>(null);

  const [
    savingCampaign,
    setSavingCampaign,
  ] = useState(false);

  const [form, setForm] = useState<CampaignFormState>(
    createEmptyCampaignForm(),
  );

  const [
    placementForm,
    setPlacementForm,
  ] = useState<PlacementRequest>(
    createEmptyPlacementForm(),
  );

  const campaignFormRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    Promise.all([
      getCampaigns(),
      getPlacements(),
    ])
      .then(
        ([
          campaignData,
          placementData,
        ]) => {
          setCampaigns(
            campaignData,
          );

          setPlacements(
            placementData,
          );
        },
      )
      .finally(() => setLoading(false));
  }, []);

  /*
   * Scroll only after React has actually
   * rendered the edit form.
   */
  useEffect(() => {
    if (
      showCampaignForm &&
      editingCampaignId !== null
    ) {
      requestAnimationFrame(() => {
        campaignFormRef.current?.scrollIntoView(
          {
            behavior: "smooth",
            block: "start",
          },
        );
      });
    }
  }, [
    showCampaignForm,
    editingCampaignId,
  ]);

  const parseCommaSeparatedValues = (
    value: string,
  ): string[] => {
    return value
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);
  };

  const getPlacementName = (
    placementId: number,
  ) => {
    const placement = placements.find(
      (item) => item.id === placementId,
    );

    return placement ? placement.name : `Placement ${placementId}`;
  };

  const getSupportedDevicesForSelectedPlacements = (
    selectedPlacementIds: number[],
  ): Set<DeviceType> => {
    const supportedDevices = new Set<DeviceType>();

    placements
      .filter((placement) =>
        selectedPlacementIds.includes(
          placement.id,
        )
      )
      .forEach(
        (placement) => {
          placement.supportedDevices.forEach(
            (device) => {
              supportedDevices.add(
                device,
              );
            },
          );
        },
      );

    return supportedDevices;
  };

  const isCampaignDeviceAvailable = (
    deviceType: DeviceType,
  ) => {
    /*
     * No placement targeting means the
     * campaign is not restricted by placement,
     * so all devices remain selectable.
     */
    if (
      form.targetPlacementIds
        .length === 0
    ) {
      return true;
    }

    const supportedDevices = getSupportedDevicesForSelectedPlacements(
      form.targetPlacementIds,
    );

    return supportedDevices.has(
      deviceType,
    );
  };

  const togglePlacement = (
    placement: Placement,
  ) => {
    /*
     * IMPORTANT:
     * inactive placements are intentionally
     * selectable.
     *
     * They simply won't be eligible for
     * actual ad serving until activated.
     */
    setForm(
      (currentForm) => {
        const alreadySelected = currentForm.targetPlacementIds.includes(
          placement.id,
        );

        const nextPlacementIds = alreadySelected
          ? currentForm.targetPlacementIds.filter(
            (id) =>
              id !==
                placement.id,
          )
          : [
            ...currentForm.targetPlacementIds,
            placement.id,
          ];

        /*
         * If no placements are selected,
         * don't restrict device targeting.
         */
        if (
          nextPlacementIds.length ===
            0
        ) {
          return {
            ...currentForm,
            targetPlacementIds: nextPlacementIds,
          };
        }

        /*
         * Remove campaign device selections
         * that are no longer supported by
         * any selected placement.
         */
        const supportedDevices = getSupportedDevicesForSelectedPlacements(
          nextPlacementIds,
        );

        const validTargetDevices = currentForm.targetDeviceTypes.filter(
          (device) =>
            supportedDevices.has(
              device,
            ),
        );

        return {
          ...currentForm,

          targetPlacementIds: nextPlacementIds,

          targetDeviceTypes: validTargetDevices,
        };
      },
    );
  };

  const toggleCampaignDevice = (
    deviceType: DeviceType,
  ) => {
    if (
      !isCampaignDeviceAvailable(
        deviceType,
      )
    ) {
      return;
    }

    setForm(
      (currentForm) => {
        const selected = currentForm.targetDeviceTypes.includes(
          deviceType,
        );

        return {
          ...currentForm,

          targetDeviceTypes: selected
            ? currentForm.targetDeviceTypes.filter(
              (device) =>
                device !==
                  deviceType,
            )
            : [
              ...currentForm.targetDeviceTypes,
              deviceType,
            ],
        };
      },
    );
  };

  const togglePlacementDevice = (
    deviceType: DeviceType,
  ) => {
    setPlacementForm(
      (currentForm) => {
        const selected = currentForm.supportedDevices.includes(
          deviceType,
        );

        return {
          ...currentForm,

          supportedDevices: selected
            ? currentForm.supportedDevices.filter(
              (device) =>
                device !==
                  deviceType,
            )
            : [
              ...currentForm.supportedDevices,
              deviceType,
            ],
        };
      },
    );
  };

  const togglePlacementCreativeType = (
    creativeType: CreativeType,
  ) => {
    setPlacementForm(
      (currentForm) => toggleCreativeTypeInForm(currentForm, creativeType),
    );
  };

  const handleCreatePlacement = async () => {
    if (
      !placementForm.name.trim() ||
      placementForm
          .supportedDevices
          .length === 0 ||
      placementForm
          .supportedCreativeTypes
          .length === 0
    ) {
      setPlacementError(
        "Name, at least one device and at least one creative type are required",
      );
      return;
    }

    try {
      setCreatingPlacement(true);

      const newPlacement = await createPlacement(
        placementForm,
      );

      setPlacements(
        (currentPlacements) => [
          ...currentPlacements,
          newPlacement,
        ],
      );

      /*
       * Newly created placement is
       * automatically selected.
       */
      setForm(
        (currentForm) => ({
          ...currentForm,

          targetPlacementIds: [
            ...currentForm.targetPlacementIds,
            newPlacement.id,
          ],
        }),
      );

      setPlacementForm(
        createEmptyPlacementForm(),
      );

      setShowPlacementModal(
        false,
      );
    } catch (requestError) {
      setPlacementError(
        toErrorMessage(requestError),
      );
    } finally {
      setCreatingPlacement(
        false,
      );
    }
  };

  const handleNewCampaign = () => {
    setEditingCampaignId(
      null,
    );

    setForm(
      createEmptyCampaignForm(),
    );

    setShowCampaignForm(
      true,
    );
  };

  const handleEditCampaign = (
    campaign: Campaign,
  ) => {
    setEditingCampaignId(
      campaign.id,
    );

    setForm({
      name: campaign.name ?? "",

      advertiser: campaign.advertiser ??
        "",

      agency: campaign.agency ?? "",

      budget: String(
        campaign.budget ?? "",
      ),

      flightStart: campaign.flightStart ??
        "",

      flightEnd: campaign.flightEnd ??
        "",

      impressionsGoal: String(
        campaign.impressionsGoal ??
          "",
      ),

      /*
       * Keep these manual inputs.
       * Needed for testing / future plans.
       */
      targetShowIds: (
        campaign.targetShowIds ??
          []
      ).join(", "),

      targetingSegments: (
        campaign.targetingSegments ??
          []
      ).join(", "),

      targetPlacementIds: [
        ...(campaign.targetPlacementIds ??
          []),
      ],

      targetDeviceTypes: [
        ...(campaign.targetDeviceTypes ??
          []),
      ],
    });

    /*
     * The useEffect above handles
     * scrolling after rendering.
     */
    setShowCampaignForm(
      true,
    );
  };

  const handleCancelCampaignForm = () => {
    setShowCampaignForm(
      false,
    );

    setEditingCampaignId(
      null,
    );

    setForm(
      createEmptyCampaignForm(),
    );
  };

  const handleSaveCampaign = async () => {
    if (
      !form.name.trim() ||
      !form.advertiser.trim() ||
      !form.agency.trim() ||
      !form.budget ||
      !form.flightStart ||
      !form.flightEnd ||
      !form.impressionsGoal
    ) {
      return;
    }

    try {
      setSavingCampaign(true);

      const targeting = {
        targetShowIds: parseCommaSeparatedValues(
          form.targetShowIds,
        ),

        targetingSegments: parseCommaSeparatedValues(
          form.targetingSegments,
        ),

        targetPlacementIds: form.targetPlacementIds,

        targetDeviceTypes: form.targetDeviceTypes,
      };

      /*
       * EDIT
       */
      if (
        editingCampaignId !==
          null
      ) {
        const existingCampaign = campaigns.find(
          (campaign) =>
            campaign.id ===
              editingCampaignId,
        );

        if (
          !existingCampaign
        ) {
          return;
        }

        const updatedCampaign = await updateCampaign(
          editingCampaignId,
          {
            ...existingCampaign,

            name: form.name.trim(),

            advertiser: form.advertiser.trim(),

            agency: form.agency.trim(),

            budget: Number(
              form.budget,
            ),

            flightStart: form.flightStart,

            flightEnd: form.flightEnd,

            impressionsGoal: Number(
              form.impressionsGoal,
            ),

            ...targeting,
          },
        );

        setCampaigns(
          (
            currentCampaigns,
          ) =>
            currentCampaigns.map(
              (campaign) =>
                campaign.id ===
                    updatedCampaign.id
                  ? updatedCampaign
                  : campaign,
            ),
        );
      } /*
       * CREATE
       */
      else {
        const newCampaign = await createCampaign(
          {
            name: form.name.trim(),

            advertiser: form.advertiser.trim(),

            agency: form.agency.trim(),

            budget: Number(
              form.budget,
            ),

            flightStart: form.flightStart,

            flightEnd: form.flightEnd,

            impressionsGoal: Number(
              form.impressionsGoal,
            ),

            ...targeting,
          },
        );

        setCampaigns(
          (
            currentCampaigns,
          ) => [
            ...currentCampaigns,
            newCampaign,
          ],
        );
      }

      handleCancelCampaignForm();
    } catch (error) {
      console.error(
        "Failed to save campaign:",
        error,
      );
    } finally {
      setSavingCampaign(false);
    }
  };

  const handleDeleteCampaign = async (
    campaign: Campaign,
  ) => {
    const confirmed = window.confirm(
      `Delete campaign "${campaign.name}"?\n\nThis permanently deletes the campaign.`,
    );

    if (!confirmed) {
      return;
    }

    try {
      await deleteCampaign(
        campaign.id,
      );

      setCampaigns(
        (
          currentCampaigns,
        ) =>
          currentCampaigns.filter(
            (
              currentCampaign,
            ) =>
              currentCampaign.id !==
                campaign.id,
          ),
      );

      if (
        editingCampaignId ===
          campaign.id
      ) {
        handleCancelCampaignForm();
      }
    } catch (error) {
      console.error(
        "Failed to delete campaign:",
        error,
      );
    }
  };

  const atRisk = campaigns.filter(
    (campaign) =>
      campaign.status ===
        "AT_RISK",
  ).length;

  const completed = campaigns.filter(
    (campaign) =>
      campaign.status ===
        "COMPLETED",
  ).length;

  const totalBudget = campaigns.reduce(
    (
      sum,
      campaign,
    ) =>
      sum +
      (campaign.budget || 0),
    0,
  );

  if (loading) {
    return (
      <div
        style={{
          padding: 24,
          color: "#888",
          fontSize: 13,
        }}
      >
        Loading...
      </div>
    );
  }

  return (
    <div
      style={{
        padding: 24,
      }}
    >
      {/* Header */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          marginBottom: 20,
        }}
      >
        <div>
          <h1
            style={{
              fontSize: 18,
              fontWeight: 500,
              color: "#e5e5e5",
              margin: 0,
            }}
          >
            Campaigns
          </h1>

          <div
            style={{
              fontSize: 12,
              color: "#777",
              marginTop: 4,
            }}
          >
            Manage campaign delivery and targeting
          </div>
        </div>

        <button
          onClick={() => {
            if (
              showCampaignForm
            ) {
              handleCancelCampaignForm();
            } else {
              handleNewCampaign();
            }
          }}
          style={primaryButtonStyle}
        >
          {showCampaignForm ? "Cancel" : "+ New Campaign"}
        </button>
      </div>

      {/* Create / Edit Campaign */}
      {showCampaignForm && (
        <div
          ref={campaignFormRef}
          style={{
            background: "#141414",

            border: "0.5px solid #2a2a2a",

            borderRadius: 12,

            padding: 16,

            marginBottom: 20,

            /*
             * Helps keep a little space
             * above the form when scrolling.
             */
            scrollMarginTop: 16,
          }}
        >
          <div
            style={{
              fontSize: 13,
              fontWeight: 500,
              color: "#e5e5e5",
              marginBottom: 12,
            }}
          >
            {editingCampaignId !==
                null
              ? "Edit campaign"
              : "New campaign"}
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(3, 1fr)",
              gap: 10,
            }}
          >
            <input
              placeholder="Campaign name"
              value={form.name}
              onChange={(
                event,
              ) =>
                setForm({
                  ...form,

                  name: event.target
                    .value,
                })}
              style={inputStyle}
            />

            <input
              placeholder="Advertiser"
              value={form.advertiser}
              onChange={(
                event,
              ) =>
                setForm({
                  ...form,

                  advertiser: event.target
                    .value,
                })}
              style={inputStyle}
            />

            <input
              placeholder="Agency"
              value={form.agency}
              onChange={(
                event,
              ) =>
                setForm({
                  ...form,

                  agency: event.target
                    .value,
                })}
              style={inputStyle}
            />

            <input
              type="number"
              placeholder="Budget"
              value={form.budget}
              onChange={(
                event,
              ) =>
                setForm({
                  ...form,

                  budget: event.target
                    .value,
                })}
              style={inputStyle}
            />

            <input
              type="date"
              value={form.flightStart}
              onChange={(
                event,
              ) =>
                setForm({
                  ...form,

                  flightStart: event.target
                    .value,
                })}
              style={{
                ...inputStyle,
                colorScheme: "dark",
              }}
            />

            <input
              type="date"
              value={form.flightEnd}
              onChange={(
                event,
              ) =>
                setForm({
                  ...form,

                  flightEnd: event.target
                    .value,
                })}
              style={{
                ...inputStyle,
                colorScheme: "dark",
              }}
            />

            <input
              type="number"
              placeholder="Impressions goal"
              value={form.impressionsGoal}
              onChange={(
                event,
              ) =>
                setForm({
                  ...form,

                  impressionsGoal: event.target
                    .value,
                })}
              style={inputStyle}
            />

            {/* KEEP FOR TESTING */}
            <input
              placeholder="Target show IDs: show-101, show-204"
              value={form.targetShowIds}
              onChange={(
                event,
              ) =>
                setForm({
                  ...form,

                  targetShowIds: event.target
                    .value,
                })}
              style={inputStyle}
            />

            {/* KEEP FOR TESTING */}
            <input
              placeholder="Audience segments: HOME_IMPROVEMENT, DIY_ENTHUSIASTS"
              value={form.targetingSegments}
              onChange={(
                event,
              ) =>
                setForm({
                  ...form,

                  targetingSegments: event.target
                    .value,
                })}
              style={inputStyle}
            />
          </div>

          {/* Placement header */}
          <div
            style={{
              marginTop: 18,
              marginBottom: 8,

              display: "flex",

              alignItems: "center",

              justifyContent: "space-between",
            }}
          >
            <div>
              <div
                style={{
                  fontSize: 12,
                  fontWeight: 500,
                  color: "#ddd",
                }}
              >
                Target placements
              </div>

              <div
                style={{
                  fontSize: 10,
                  color: "#666",
                  marginTop: 2,
                }}
              >
                Active and inactive placements may both be targeted. Inactive
                placements will only serve after activation.
              </div>
            </div>

            <button
              type="button"
              onClick={() => {
                setPlacementError(null);
                setShowPlacementModal(true);
              }}
              style={secondaryButtonStyle}
            >
              + Request custom placement
            </button>
          </div>

          {/* Placements */}
          {placements.length ===
              0
            ? (
              <div
                style={{
                  border: "0.5px dashed #333",

                  borderRadius: 8,

                  padding: 18,

                  textAlign: "center",

                  fontSize: 11,

                  color: "#777",
                }}
              >
                No placements exist yet. Request one without leaving this.
              </div>
            )
            : (
              <div
                style={{
                  display: "grid",

                  gridTemplateColumns: "repeat(3, minmax(0, 1fr))",

                  gap: 10,
                }}
              >
                {placements.map(
                  (placement) => {
                    const selected = form.targetPlacementIds.includes(
                      placement.id,
                    );

                    return (
                      <button
                        key={placement.id}
                        type="button"
                        onClick={() =>
                          togglePlacement(
                            placement,
                          )}
                        style={{
                          textAlign: "left",

                          background: selected ? "#251618" : "#181818",

                          border: selected
                            ? "1px solid #E50914"
                            : "0.5px solid #303030",

                          borderRadius: 8,

                          padding: 11,

                          cursor: "pointer",

                          opacity: 1,
                        }}
                      >
                        <div
                          style={{
                            display: "flex",

                            justifyContent: "space-between",

                            alignItems: "flex-start",

                            gap: 8,
                          }}
                        >
                          <div>
                            <div
                              style={{
                                fontSize: 12,

                                fontWeight: 500,

                                color: "#e5e5e5",
                              }}
                            >
                              {placement.name}
                            </div>

                            <div
                              style={{
                                fontSize: 10,

                                color: "#8fa9ff",

                                marginTop: 3,
                              }}
                            >
                              {placement.type.replaceAll(
                                "_",
                                " ",
                              )}
                            </div>
                          </div>

                          <span
                            style={{
                              fontSize: 9,

                              color: placement.active ? "#86efac" : "#fbbf24",

                              background: placement.active
                                ? "#17251d"
                                : "#2b2416",

                              border: "0.5px solid #333",

                              borderRadius: 5,

                              padding: "2px 5px",
                            }}
                          >
                            {placement.active ? "ACTIVE" : "INACTIVE"}
                          </span>
                        </div>

                        <div
                          style={{
                            marginTop: 9,

                            fontSize: 10,

                            color: "#888",
                          }}
                        >
                          Devices
                        </div>

                        <div
                          style={{
                            display: "flex",

                            flexWrap: "wrap",

                            gap: 4,

                            marginTop: 4,
                          }}
                        >
                          {placement.supportedDevices.map(
                            (device) => (
                              <span
                                key={device}
                                style={miniChipStyle}
                              >
                                {device}
                              </span>
                            ),
                          )}
                        </div>

                        <div
                          style={{
                            marginTop: 8,

                            fontSize: 10,

                            color: "#888",
                          }}
                        >
                          Creative types
                        </div>

                        <div
                          style={{
                            display: "flex",

                            flexWrap: "wrap",

                            gap: 4,

                            marginTop: 4,
                          }}
                        >
                          {placement.supportedCreativeTypes.map(
                            (type) => (
                              <span
                                key={type}
                                style={miniChipStyle}
                              >
                                {type}
                              </span>
                            ),
                          )}
                        </div>

                        <div
                          style={{
                            display: "flex",

                            gap: 12,

                            marginTop: 9,

                            color: "#777",

                            fontSize: 10,
                          }}
                        >
                          <span>
                            Max ad length: {placement.maxAdDurationSeconds ??
                              "—"}
                            {placement.maxAdDurationSeconds ? "s" : ""}
                          </span>

                          <span>
                            Max ads: {placement.maxAds ??
                              "—"}
                          </span>
                        </div>

                        {!placement.active && (
                          <div
                            style={{
                              fontSize: 9,

                              color: "#fbbf24",

                              marginTop: 7,
                            }}
                          >
                            Currently inactive — eligible only after activation
                          </div>
                        )}

                        {selected && (
                          <div
                            style={{
                              marginTop: 8,

                              fontSize: 10,

                              color: "#ff8d93",

                              fontWeight: 500,
                            }}
                          >
                            ✓ Selected
                          </div>
                        )}
                      </button>
                    );
                  },
                )}
              </div>
            )}

          {/* Campaign devices */}
          <div
            style={{
              marginTop: 16,
            }}
          >
            <div
              style={{
                fontSize: 12,
                fontWeight: 500,
                color: "#ddd",
                marginBottom: 3,
              }}
            >
              Target devices
            </div>

            <div
              style={{
                fontSize: 10,
                color: "#666",
                marginBottom: 7,
              }}
            >
              A device must be supported by at least one selected placement.
              Leave this empty to allow all supported devices.
            </div>

            <div
              style={{
                display: "flex",
                flexWrap: "wrap",
                gap: 7,
              }}
            >
              {deviceTypes.map(
                (deviceType) => {
                  const selected = form.targetDeviceTypes.includes(
                    deviceType,
                  );

                  const available = isCampaignDeviceAvailable(
                    deviceType,
                  );

                  return (
                    <button
                      key={deviceType}
                      type="button"
                      disabled={!available}
                      title={available
                        ? undefined
                        : "None of the selected placements supports this device"}
                      onClick={() =>
                        toggleCampaignDevice(
                          deviceType,
                        )}
                      style={{
                        ...targetButtonStyle,

                        border: selected
                          ? "1px solid #E50914"
                          : "0.5px solid #333",

                        background: selected ? "#301416" : "#1a1a1a",

                        color: selected ? "#fff" : "#aaa",

                        opacity: available ? 1 : 0.35,

                        cursor: available ? "pointer" : "not-allowed",
                      }}
                    >
                      {deviceType}
                    </button>
                  );
                },
              )}
            </div>
          </div>

          <div
            style={{
              fontSize: 11,
              color: "#666",
              marginTop: 10,
            }}
          >
            Leave a targeting category empty to allow all values in that
            category
          </div>

          <div
            style={{
              display: "flex",
              gap: 8,
              marginTop: 14,
            }}
          >
            <button
              type="button"
              onClick={handleSaveCampaign}
              disabled={savingCampaign}
              style={{
                ...primaryButtonStyle,

                opacity: savingCampaign ? 0.6 : 1,
              }}
            >
              {savingCampaign ? "Saving..." : editingCampaignId !==
                  null
                ? "Save changes"
                : "Create campaign"}
            </button>

            {editingCampaignId !==
                null && (
              <button
                type="button"
                onClick={handleCancelCampaignForm}
                style={secondaryButtonStyle}
              >
                Cancel edit
              </button>
            )}
          </div>
        </div>
      )}

      {/* Metrics */}
      <div
        style={{
          display: "grid",

          gridTemplateColumns: "repeat(4, 1fr)",

          gap: 12,

          marginBottom: 24,
        }}
      >
        <MetricCard
          label="Total campaigns"
          value={campaigns.length}
          delta="All time"
          deltaType="neutral"
        />

        <MetricCard
          label="Total budget"
          value={`$${
            (
              totalBudget /
              1_000_000
            ).toFixed(1)
          }M`}
          delta="Combined"
          deltaType="neutral"
        />

        <MetricCard
          label="At risk"
          value={atRisk}
          delta={atRisk > 0 ? "Needs attention" : "All good"}
          deltaType={atRisk > 0 ? "down" : "up"}
        />

        <MetricCard
          label="Completed"
          value={completed}
          deltaType="neutral"
        />
      </div>

      {/* Campaign list */}
      <div
        style={{
          background: "#141414",

          border: "0.5px solid #1e1e1e",

          borderRadius: 12,

          padding: "14px 16px",
        }}
      >
        <div
          style={{
            fontSize: 13,

            fontWeight: 500,

            color: "#e5e5e5",

            marginBottom: 12,
          }}
        >
          All campaigns
        </div>

        {campaigns.length ===
            0
          ? (
            <div
              style={{
                fontSize: 13,
                color: "#888",
                padding: "12px 0",
              }}
            >
              No campaigns yet. Create your first one above.
            </div>
          )
          : (
            campaigns.map(
              (campaign) => (
                <div
                  key={campaign.id}
                  style={{
                    display: "flex",

                    alignItems: "center",

                    gap: 12,

                    padding: "12px 0",

                    borderBottom: "0.5px solid #1e1e1e",

                    fontSize: 13,
                  }}
                >
                  <div
                    style={{
                      flex: 1,
                      minWidth: 0,
                    }}
                  >
                    <div
                      style={{
                        fontWeight: 500,

                        color: "#e5e5e5",
                      }}
                    >
                      {campaign.name}
                    </div>

                    <div
                      style={{
                        fontSize: 11,

                        color: "#888",

                        marginTop: 2,
                      }}
                    >
                      {campaign.agency}
                      {" · "}

                      $
                      {(
                        campaign.budget /
                        1_000_000
                      ).toFixed(1)}
                      M

                      {" · "}

                      {campaign.flightStart}

                      {" – "}

                      {campaign.flightEnd}
                    </div>

                    <div
                      style={{
                        display: "flex",

                        flexWrap: "wrap",

                        gap: 5,

                        marginTop: 7,
                      }}
                    >
                      {(
                        campaign.targetShowIds ??
                          []
                      ).map(
                        (showId) => (
                          <span
                            key={`show-${campaign.id}-${showId}`}
                            style={targetChipStyle}
                          >
                            Show: {showId}
                          </span>
                        ),
                      )}

                      {(
                        campaign.targetingSegments ??
                          []
                      ).map(
                        (
                          segment,
                        ) => (
                          <span
                            key={`segment-${campaign.id}-${segment}`}
                            style={segmentChipStyle}
                          >
                            {segment}
                          </span>
                        ),
                      )}

                      {(
                        campaign.targetPlacementIds ??
                          []
                      ).map(
                        (
                          placementId,
                        ) => (
                          <span
                            key={`placement-${campaign.id}-${placementId}`}
                            style={placementChipStyle}
                          >
                            Placement: {getPlacementName(
                              placementId,
                            )}
                          </span>
                        ),
                      )}

                      {(
                        campaign.targetDeviceTypes ??
                          []
                      ).map(
                        (
                          deviceType,
                        ) => (
                          <span
                            key={`device-${campaign.id}-${deviceType}`}
                            style={deviceChipStyle}
                          >
                            Device: {deviceType}
                          </span>
                        ),
                      )}

                      {(campaign.targetShowIds ??
                            []).length ===
                          0 &&
                        (
                            campaign.targetingSegments ??
                              []
                          ).length ===
                          0 &&
                        (
                            campaign.targetPlacementIds ??
                              []
                          ).length ===
                          0 &&
                        (
                            campaign.targetDeviceTypes ??
                              []
                          ).length ===
                          0 &&
                        (
                          <span
                            style={{
                              fontSize: 10,

                              color: "#666",
                            }}
                          >
                            No targeting configured
                          </span>
                        )}
                    </div>
                  </div>

                  <StatusBadge
                    status={campaign.status}
                  />

                  <div
                    style={{
                      width: 80,

                      height: 5,

                      background: "#2a2a2a",

                      borderRadius: 99,

                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        height: "100%",

                        borderRadius: 99,

                        width: `${
                          campaign.pacingPercent ??
                            0
                        }%`,

                        background: (campaign.pacingPercent ??
                            0) >=
                            70
                          ? "#4ade80"
                          : (campaign.pacingPercent ??
                              0) >=
                              40
                          ? "#fbbf24"
                          : "#f87171",
                      }}
                    />
                  </div>

                  <div
                    style={{
                      fontSize: 11,

                      color: "#888",

                      width: 36,

                      textAlign: "right",
                    }}
                  >
                    {campaign.pacingPercent?.toFixed(
                      0,
                    ) ?? 0}
                    %
                  </div>

                  <div
                    style={{
                      display: "flex",

                      gap: 6,
                    }}
                  >
                    <button
                      type="button"
                      onClick={() =>
                        handleEditCampaign(
                          campaign,
                        )}
                      style={secondaryButtonStyle}
                    >
                      Edit
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        handleDeleteCampaign(
                          campaign,
                        )}
                      style={deleteButtonStyle}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              ),
            )
          )}
      </div>

      {/* Create Placement Modal */}
      {showPlacementModal && (
        <div
          style={modalOverlayStyle}
          onClick={() =>
            setShowPlacementModal(
              false,
            )}
        >
          <div
            style={modalStyle}
            onClick={(event) => event.stopPropagation()}
          >
            <div
              style={{
                display: "flex",

                justifyContent: "space-between",

                alignItems: "center",

                marginBottom: 16,
              }}
            >
              <div>
                <div
                  style={{
                    fontSize: 15,

                    fontWeight: 500,

                    color: "#e5e5e5",
                  }}
                >
                  Request custom placement
                </div>

                <div
                  style={{
                    fontSize: 11,

                    color: "#777",

                    marginTop: 3,
                  }}
                >
                  A requested placement starts inactive and becomes available
                  for serving once ad ops activates it on the Placements page.
                </div>
              </div>

              <button
                type="button"
                onClick={() =>
                  setShowPlacementModal(
                    false,
                  )}
                style={{
                  border: "none",

                  background: "transparent",

                  color: "#888",

                  fontSize: 18,

                  cursor: "pointer",
                }}
              >
                ×
              </button>
            </div>

            <div
              style={{
                display: "grid",

                gridTemplateColumns: "1fr 1fr",

                gap: 10,
              }}
            >
              <input
                placeholder="Placement name"
                value={placementForm.name}
                onChange={(
                  event,
                ) =>
                  setPlacementForm({
                    ...placementForm,

                    name: event.target
                      .value,
                  })}
                style={inputStyle}
              />

              <select
                value={placementForm.type}
                onChange={(
                  event,
                ) =>
                  setPlacementForm({
                    ...placementForm,

                    type: event.target
                      .value as PlacementType,
                  })}
                style={inputStyle}
              >
                {placementTypes.map(
                  (type) => (
                    <option
                      key={type}
                      value={type}
                    >
                      {type.replaceAll(
                        "_",
                        " ",
                      )}
                    </option>
                  ),
                )}
              </select>

              <input
                type="number"
                min={1}
                placeholder="Max ads"
                value={placementForm.maxAds ??
                  ""}
                onChange={(
                  event,
                ) =>
                  setPlacementForm({
                    ...placementForm,

                    maxAds: event.target.value ===
                        ""
                      ? null
                      : Number(
                        event.target
                          .value,
                      ),
                  })}
                style={inputStyle}
              />

              {/* A max ad duration only applies when the placement accepts video */}
              {acceptsVideo(placementForm.supportedCreativeTypes) && (
                <input
                  type="number"
                  min={1}
                  placeholder="Max ad duration (seconds)"
                  value={placementForm.maxAdDurationSeconds ??
                    ""}
                  onChange={(
                    event,
                  ) =>
                    setPlacementForm({
                      ...placementForm,

                      maxAdDurationSeconds: event.target.value ===
                          ""
                        ? null
                        : Number(
                          event.target
                            .value,
                        ),
                    })}
                  style={inputStyle}
                />
              )}
            </div>

            {/* Supported devices */}
            <div
              style={{
                marginTop: 16,
              }}
            >
              <div
                style={targetSectionLabelStyle}
              >
                Supported devices
              </div>

              <div
                style={{
                  display: "flex",

                  flexWrap: "wrap",

                  gap: 7,
                }}
              >
                {deviceTypes.map(
                  (deviceType) => {
                    const selected = placementForm.supportedDevices.includes(
                      deviceType,
                    );

                    return (
                      <button
                        key={deviceType}
                        type="button"
                        onClick={() =>
                          togglePlacementDevice(
                            deviceType,
                          )}
                        style={{
                          ...targetButtonStyle,

                          border: selected
                            ? "1px solid #E50914"
                            : "0.5px solid #333",

                          background: selected ? "#301416" : "#1a1a1a",

                          color: selected ? "#fff" : "#aaa",
                        }}
                      >
                        {deviceType}
                      </button>
                    );
                  },
                )}
              </div>
            </div>

            {/* Creative types */}
            <div
              style={{
                marginTop: 16,
              }}
            >
              <div
                style={targetSectionLabelStyle}
              >
                Supported creative types
              </div>

              <div
                style={{
                  display: "flex",

                  flexWrap: "wrap",

                  gap: 7,
                }}
              >
                {creativeTypes.map(
                  (
                    creativeType,
                  ) => {
                    const selected = placementForm.supportedCreativeTypes
                      .includes(
                        creativeType,
                      );

                    return (
                      <button
                        key={creativeType}
                        type="button"
                        onClick={() =>
                          togglePlacementCreativeType(
                            creativeType,
                          )}
                        style={{
                          ...targetButtonStyle,

                          border: selected
                            ? "1px solid #E50914"
                            : "0.5px solid #333",

                          background: selected ? "#301416" : "#1a1a1a",

                          color: selected ? "#fff" : "#aaa",
                        }}
                      >
                        {creativeType}
                      </button>
                    );
                  },
                )}
              </div>
            </div>

            {placementError && (
              <div
                role="alert"
                style={{
                  fontSize: 12,
                  color: "#f0a0a0",
                  background: "#2a1515",
                  border: "0.5px solid #5a2a2a",
                  borderRadius: 6,
                  padding: "8px 12px",
                  marginTop: 16,
                }}
              >
                {placementError}
              </div>
            )}

            <div
              style={{
                display: "flex",

                justifyContent: "flex-end",

                gap: 8,

                marginTop: 20,
              }}
            >
              <button
                type="button"
                onClick={() =>
                  setShowPlacementModal(
                    false,
                  )}
                style={secondaryButtonStyle}
              >
                Cancel
              </button>

              <button
                type="button"
                disabled={creatingPlacement}
                onClick={handleCreatePlacement}
                style={{
                  ...primaryButtonStyle,

                  opacity: creatingPlacement ? 0.6 : 1,
                }}
              >
                {creatingPlacement ? "Requesting..." : "Request & select"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  fontSize: 12,
  background: "#1a1a1a",
  color: "#e5e5e5",
  border: "0.5px solid #2a2a2a",
  borderRadius: 6,
  padding: "7px 10px",
  outline: "none",
};

const primaryButtonStyle: React.CSSProperties = {
  fontSize: 12,
  background: "#E50914",
  color: "#fff",
  border: "none",
  borderRadius: 6,
  padding: "6px 14px",
  fontWeight: 500,
  cursor: "pointer",
};

const secondaryButtonStyle: React.CSSProperties = {
  fontSize: 11,
  background: "#1a1a1a",
  color: "#ddd",
  border: "0.5px solid #3a3a3a",
  borderRadius: 6,
  padding: "6px 10px",
  cursor: "pointer",
};

const deleteButtonStyle: React.CSSProperties = {
  fontSize: 11,
  background: "#241416",
  color: "#ff8d93",
  border: "0.5px solid #5b2328",
  borderRadius: 6,
  padding: "6px 10px",
  cursor: "pointer",
};

const targetSectionLabelStyle: React.CSSProperties = {
  fontSize: 11,
  color: "#888",
  marginBottom: 7,
};

const targetButtonStyle: React.CSSProperties = {
  fontSize: 11,
  borderRadius: 6,
  padding: "5px 9px",
  cursor: "pointer",
};

const miniChipStyle: React.CSSProperties = {
  fontSize: 9,
  color: "#ccc",
  background: "#242424",
  border: "0.5px solid #333",
  borderRadius: 4,
  padding: "2px 5px",
};

const targetChipStyle: React.CSSProperties = {
  fontSize: 10,
  color: "#b8c7ff",
  background: "#1b2033",
  border: "0.5px solid #283457",
  borderRadius: 5,
  padding: "2px 6px",
};

const segmentChipStyle: React.CSSProperties = {
  fontSize: 10,
  color: "#d4d4d4",
  background: "#202020",
  border: "0.5px solid #333",
  borderRadius: 5,
  padding: "2px 6px",
};

const placementChipStyle: React.CSSProperties = {
  fontSize: 10,
  color: "#ffd6a5",
  background: "#2a2118",
  border: "0.5px solid #493821",
  borderRadius: 5,
  padding: "2px 6px",
};

const deviceChipStyle: React.CSSProperties = {
  fontSize: 10,
  color: "#b8f2d0",
  background: "#17251d",
  border: "0.5px solid #264932",
  borderRadius: 5,
  padding: "2px 6px",
};

const modalOverlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  background: "rgba(0, 0, 0, 0.72)",

  display: "flex",

  alignItems: "center",

  justifyContent: "center",

  zIndex: 1000,
};

const modalStyle: React.CSSProperties = {
  width: 560,

  maxWidth: "calc(100vw - 40px)",

  background: "#141414",

  border: "0.5px solid #333",

  borderRadius: 12,

  padding: 18,

  boxShadow: "0 20px 60px rgba(0, 0, 0, 0.45)",
};
