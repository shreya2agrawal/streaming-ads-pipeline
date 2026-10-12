import React, { FormEvent, useEffect, useState } from "react";
import MetricCard from "../components/MetricCard";

import {
  createCreative,
  getCreatives,
  updateCreative,
  updateCreativeStatus,
} from "../api/creatives";

import {
  decideSafetyReview,
  getSafetyReviewsForCreative,
  submitCreativeForSafetyReview,
} from "../api/safetyReviews";

import { getCampaigns } from "../api/campaigns";

import {
  Campaign,
  CreateCreativeRequest,
  Creative,
  CreativeSafetyReview,
  CreativeSafetyStatus,
  CreativeStatus,
  CreativeType,
  UpdateCreativeRequest,
} from "../types";

const creativeTypes: CreativeType[] = [
  "VIDEO",
  "IMAGE",
  "NATIVE",
];

const getInitialForm = (): CreateCreativeRequest => ({
  campaignId: 0,
  name: "",
  creativeType: "VIDEO",
  mediaUrl: "",
  destinationUrl: "",
  contentText: "",
  durationSeconds: 30,
});

const inputStyle: React.CSSProperties = {
  fontSize: 12,
  background: "#1a1a1a",
  color: "#e5e5e5",
  border: "0.5px solid #2a2a2a",
  borderRadius: 6,
  padding: "7px 10px",
  width: "100%",
  outline: "none",
};

const primaryButtonStyle: React.CSSProperties = {
  fontSize: 12,
  background: "#E50914",
  color: "#fff",
  border: "none",
  borderRadius: 6,
  padding: "7px 16px",
  cursor: "pointer",
  fontWeight: 500,
};

const secondaryButtonStyle: React.CSSProperties = {
  fontSize: 11,
  color: "#aaa",
  background: "#1e1e1e",
  border: "0.5px solid #2a2a2a",
  borderRadius: 6,
  padding: "5px 10px",
  cursor: "pointer",
};

const getSafetyStyle = (
  status: CreativeSafetyStatus,
): React.CSSProperties => {
  switch (status) {
    case "APPROVED":
      return {
        background: "rgba(74, 222, 128, 0.1)",
        color: "#4ade80",
      };

    case "REJECTED":
      return {
        background: "rgba(248, 113, 113, 0.1)",
        color: "#f87171",
      };

    case "PENDING_REVIEW":
      return {
        background: "rgba(251, 191, 36, 0.1)",
        color: "#fbbf24",
      };

    default:
      return {
        background: "#1e1e1e",
        color: "#888",
      };
  }
};

const getLifecycleStyle = (
  status: CreativeStatus,
): React.CSSProperties => {
  switch (status) {
    case "ACTIVE":
      return {
        background: "rgba(74, 222, 128, 0.1)",
        color: "#4ade80",
      };

    case "PAUSED":
      return {
        background: "rgba(251, 191, 36, 0.1)",
        color: "#fbbf24",
      };

    case "ARCHIVED":
      return {
        background: "#1e1e1e",
        color: "#666",
      };

    default:
      return {
        background: "#1e1e1e",
        color: "#888",
      };
  }
};

export default function CreativesPage() {
  const [creatives, setCreatives] = useState<Creative[]>([]);
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);

  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);

  const [editingCreative, setEditingCreative] = useState<Creative | null>(null);

  const [form, setForm] = useState<CreateCreativeRequest>(getInitialForm());

  const [reviewHistory, setReviewHistory] = useState<
    Record<number, CreativeSafetyReview[]>
  >({});

  const [expandedCreativeId, setExpandedCreativeId] = useState<number | null>(
    null,
  );

  const [reviewer, setReviewer] = useState("");
  const [reviewReason, setReviewReason] = useState("");

  const [actionCreativeId, setActionCreativeId] = useState<number | null>(null);

  const [error, setError] = useState("");

  const loadCreatives = async () => {
    const data = await getCreatives();
    setCreatives(data);
  };

  const loadCampaigns = async () => {
    const data = await getCampaigns();
    setCampaigns(data);
  };

  const loadPage = async () => {
    try {
      setLoading(true);
      setError("");

      await Promise.all([
        loadCreatives(),
        loadCampaigns(),
      ]);
    } catch (error) {
      console.error(error);
      setError("Unable to load creative library");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPage();
  }, []);

  const resetForm = () => {
    setForm(getInitialForm());
    setEditingCreative(null);
    setShowForm(false);
  };

  const handleNewCreative = () => {
    setEditingCreative(null);
    setForm(getInitialForm());
    setShowForm(true);
    setError("");
  };

  const handleEdit = (creative: Creative) => {
    if (creative.safetyStatus === "PENDING_REVIEW") {
      setError(
        "Creative cannot be edited while safety review is pending",
      );
      return;
    }

    setEditingCreative(creative);

    setForm({
      campaignId: creative.campaignId,
      name: creative.name,
      creativeType: creative.creativeType,
      mediaUrl: creative.mediaUrl,
      destinationUrl: creative.destinationUrl,
      contentText: creative.contentText,
      durationSeconds: creative.durationSeconds,
    });

    setShowForm(true);
    setError("");

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const handleSubmit = async (
    event: FormEvent,
  ) => {
    event.preventDefault();

    if (
      !form.campaignId ||
      !form.name.trim() ||
      !form.mediaUrl.trim() ||
      !form.destinationUrl.trim() ||
      !form.contentText.trim()
    ) {
      return;
    }

    try {
      setSaving(true);
      setError("");

      if (editingCreative) {
        const confirmed = window.confirm(
          `Save changes to "${form.name}"? Editing creative content will reset its safety approval.`,
        );

        if (!confirmed) {
          setSaving(false);
          return;
        }

        const request: UpdateCreativeRequest = {
          name: form.name,
          creativeType: form.creativeType,
          mediaUrl: form.mediaUrl,
          destinationUrl: form.destinationUrl,
          contentText: form.contentText,
          durationSeconds: form.durationSeconds,
        };

        const updatedCreative = await updateCreative(
          editingCreative.id,
          request,
        );

        setCreatives((currentCreatives) =>
          currentCreatives.map((creative) =>
            creative.id === updatedCreative.id ? updatedCreative : creative
          )
        );
      } else {
        const newCreative = await createCreative(form);

        setCreatives((currentCreatives) => [
          ...currentCreatives,
          newCreative,
        ]);
      }

      resetForm();
    } catch (error) {
      console.error(error);
      setError("Unable to save creative");
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async (
    creative: Creative,
    status: CreativeStatus,
  ) => {
    if (status === creative.status) {
      return;
    }

    const confirmed = window.confirm(
      `Change "${creative.name}" from ${creative.status} to ${status}?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      setActionCreativeId(creative.id);
      setError("");

      const updatedCreative = await updateCreativeStatus(
        creative.id,
        status,
      );

      setCreatives((currentCreatives) =>
        currentCreatives.map((currentCreative) =>
          currentCreative.id === updatedCreative.id
            ? updatedCreative
            : currentCreative
        )
      );
    } catch (error) {
      console.error(error);
      setError(
        "Unable to update creative lifecycle status",
      );
    } finally {
      setActionCreativeId(null);
    }
  };

  const loadReviewHistory = async (
    creativeId: number,
  ) => {
    try {
      const reviews = await getSafetyReviewsForCreative(
        creativeId,
      );

      setReviewHistory((currentHistory) => ({
        ...currentHistory,
        [creativeId]: reviews,
      }));
    } catch (error) {
      console.error(error);
      setError(
        "Unable to load safety review history",
      );
    }
  };

  const handleSubmitForReview = async (
    creative: Creative,
  ) => {
    try {
      setActionCreativeId(creative.id);
      setError("");

      await submitCreativeForSafetyReview(
        creative.id,
      );

      await Promise.all([
        loadCreatives(),
        loadReviewHistory(creative.id),
      ]);

      setExpandedCreativeId(creative.id);
    } catch (error) {
      console.error(error);
      setError(
        "Unable to submit creative for safety review",
      );
    } finally {
      setActionCreativeId(null);
    }
  };

  const handleToggleReviewPanel = async (
    creativeId: number,
  ) => {
    if (expandedCreativeId === creativeId) {
      setExpandedCreativeId(null);
      return;
    }

    setExpandedCreativeId(creativeId);
    setReviewer("");
    setReviewReason("");
    setError("");

    if (!reviewHistory[creativeId]) {
      await loadReviewHistory(creativeId);
    }
  };

  const handleReviewDecision = async (
    creativeId: number,
    reviewId: number,
    status: "APPROVED" | "REJECTED",
  ) => {
    if (!reviewer.trim()) {
      setError("Reviewer name is required");
      return;
    }

    const confirmed = window.confirm(
      `${status === "APPROVED" ? "Approve" : "Reject"} this creative?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      setActionCreativeId(creativeId);
      setError("");

      await decideSafetyReview(reviewId, {
        status,
        reviewer: reviewer.trim(),
        reason: reviewReason.trim() || null,
      });

      setReviewer("");
      setReviewReason("");

      await Promise.all([
        loadCreatives(),
        loadReviewHistory(creativeId),
      ]);
    } catch (error) {
      console.error(error);
      setError(
        "Unable to save safety review decision",
      );
    } finally {
      setActionCreativeId(null);
    }
  };

  const updateCreativeType = (
    creativeType: CreativeType,
  ) => {
    setForm((currentForm) => ({
      ...currentForm,
      creativeType,
      durationSeconds: creativeType === "VIDEO"
        ? currentForm.durationSeconds ?? 30
        : null,
    }));
  };

  const formatDate = (
    value: string | null,
  ) => {
    if (!value) {
      return "—";
    }

    return new Date(value).toLocaleString();
  };

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

  const activeCount = creatives.filter(
    (creative) => creative.status === "ACTIVE",
  ).length;

  const approvedCount = creatives.filter(
    (creative) => creative.safetyStatus === "APPROVED",
  ).length;

  const pendingCount = creatives.filter(
    (creative) =>
      creative.safetyStatus ===
        "PENDING_REVIEW",
  ).length;

  return (
    <div style={{ padding: 24 }}>
      {/* Top bar */}

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
            Creative Library
          </h1>

          <div
            style={{
              fontSize: 12,
              color: "#888",
              marginTop: 4,
            }}
          >
            Manage streaming ad creatives, lifecycle status and human safety
            review
          </div>
        </div>

        <button
          onClick={showForm ? resetForm : handleNewCreative}
          style={{
            fontSize: 12,
            background: showForm ? "#242424" : "#E50914",
            color: "#fff",
            border: showForm ? "0.5px solid #333" : "none",
            borderRadius: 6,
            padding: "6px 14px",
            fontWeight: 500,
            cursor: "pointer",
          }}
        >
          {showForm ? "Cancel" : "+ New Creative"}
        </button>
      </div>

      {/* Error */}

      {error && (
        <div
          style={{
            fontSize: 12,
            color: "#f87171",
            background: "rgba(248, 113, 113, 0.08)",
            border: "0.5px solid rgba(248, 113, 113, 0.25)",
            borderRadius: 8,
            padding: "9px 12px",
            marginBottom: 20,
          }}
        >
          {error}
        </div>
      )}

      {/* Create / Edit form */}

      {showForm && (
        <form
          onSubmit={handleSubmit}
          style={{
            background: "#141414",
            border: "0.5px solid #2a2a2a",
            borderRadius: 12,
            padding: 16,
            marginBottom: 20,
          }}
        >
          <div
            style={{
              fontSize: 13,
              fontWeight: 500,
              color: "#e5e5e5",
              marginBottom: 4,
            }}
          >
            {editingCreative ? "Edit creative" : "New creative"}
          </div>

          <div
            style={{
              fontSize: 11,
              color: "#777",
              marginBottom: 14,
            }}
          >
            {editingCreative
              ? "Material changes require the creative to pass safety review again"
              : "Add a creative asset to an existing campaign"}
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "2fr 2fr 1fr 1fr",
              gap: 10,
              marginBottom: 10,
            }}
          >
            <select
              value={form.campaignId || ""}
              disabled={!!editingCreative}
              required
              onChange={(event) =>
                setForm({
                  ...form,
                  campaignId: Number(
                    event.target.value,
                  ),
                })}
              style={{
                ...inputStyle,
                opacity: editingCreative ? 0.6 : 1,
              }}
            >
              <option value="">
                Select campaign
              </option>

              {campaigns.map((campaign) => (
                <option
                  key={campaign.id}
                  value={campaign.id}
                >
                  {campaign.name} · {campaign.advertiser}
                </option>
              ))}
            </select>

            <input
              placeholder="Creative name"
              value={form.name}
              required
              onChange={(event) =>
                setForm({
                  ...form,
                  name: event.target.value,
                })}
              style={inputStyle}
            />

            <select
              value={form.creativeType}
              onChange={(event) =>
                updateCreativeType(
                  event.target
                    .value as CreativeType,
                )}
              style={inputStyle}
            >
              {creativeTypes.map(
                (creativeType) => (
                  <option
                    key={creativeType}
                    value={creativeType}
                  >
                    {creativeType}
                  </option>
                ),
              )}
            </select>

            {form.creativeType === "VIDEO"
              ? (
                <input
                  type="number"
                  min="1"
                  placeholder="Duration (seconds)"
                  value={form.durationSeconds ?? ""}
                  required
                  onChange={(event) =>
                    setForm({
                      ...form,
                      durationSeconds: event.target.value === ""
                        ? null
                        : Number(
                          event.target.value,
                        ),
                    })}
                  style={inputStyle}
                />
              )
              : <div />}
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: 10,
              marginBottom: 10,
            }}
          >
            <input
              type="url"
              placeholder="Media URL"
              value={form.mediaUrl}
              required
              onChange={(event) =>
                setForm({
                  ...form,
                  mediaUrl: event.target.value,
                })}
              style={inputStyle}
            />

            <input
              type="url"
              placeholder="Destination URL"
              value={form.destinationUrl}
              required
              onChange={(event) =>
                setForm({
                  ...form,
                  destinationUrl: event.target.value,
                })}
              style={inputStyle}
            />
          </div>

          <textarea
            placeholder="Creative content / ad copy"
            value={form.contentText}
            required
            rows={3}
            onChange={(event) =>
              setForm({
                ...form,
                contentText: event.target.value,
              })}
            style={{
              ...inputStyle,
              resize: "vertical",
              minHeight: 72,
              marginBottom: 12,
            }}
          />

          <div
            style={{
              display: "flex",
              gap: 8,
            }}
          >
            <button
              type="submit"
              disabled={saving}
              style={{
                ...primaryButtonStyle,
                opacity: saving ? 0.6 : 1,
              }}
            >
              {saving
                ? "Saving..."
                : editingCreative
                ? "Save changes"
                : "Create creative"}
            </button>

            <button
              type="button"
              onClick={resetForm}
              style={{
                fontSize: 12,
                background: "#242424",
                color: "#aaa",
                border: "0.5px solid #333",
                borderRadius: 6,
                padding: "7px 16px",
              }}
            >
              Cancel
            </button>
          </div>
        </form>
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
          label="Total creatives"
          value={creatives.length}
          delta="Library assets"
          deltaType="neutral"
        />

        <MetricCard
          label="Active"
          value={activeCount}
          delta="Lifecycle active"
          deltaType="up"
        />

        <MetricCard
          label="Safety approved"
          value={approvedCount}
          delta="Eligible after policy checks"
          deltaType="up"
        />

        <MetricCard
          label="Pending review"
          value={pendingCount}
          delta={pendingCount > 0 ? "Needs attention" : "Queue clear"}
          deltaType={pendingCount > 0 ? "down" : "neutral"}
        />
      </div>

      {/* Creative library */}

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
          Creative assets
        </div>

        {creatives.length === 0
          ? (
            <div
              style={{
                color: "#888",
                fontSize: 12,
                padding: "20px 0",
              }}
            >
              No creatives created yet
            </div>
          )
          : (
            creatives.map((creative) => {
              const reviews = reviewHistory[creative.id] ?? [];

              const pendingReview = reviews.find(
                (review) => review.status === "PENDING",
              );

              const expanded = expandedCreativeId ===
                creative.id;

              const lifecycleStyle = getLifecycleStyle(
                creative.status,
              );

              const safetyStyle = getSafetyStyle(
                creative.safetyStatus,
              );

              return (
                <div
                  key={creative.id}
                  style={{
                    borderBottom: "0.5px solid #1e1e1e",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 16,
                      padding: "12px 0",
                      fontSize: 13,
                    }}
                  >
                    {/* Main info */}

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
                        {creative.name}
                      </div>

                      <div
                        style={{
                          fontSize: 11,
                          color: "#888",
                          marginTop: 3,
                        }}
                      >
                        Campaign #
                        {creative.campaignId}
                        {" · "}
                        {creative.creativeType}

                        {creative.durationSeconds !==
                            null &&
                          ` · ${creative.durationSeconds}s`}
                      </div>
                    </div>

                    {/* Lifecycle */}

                    <div
                      style={{
                        width: 100,
                      }}
                    >
                      <div
                        style={{
                          fontSize: 10,
                          color: "#666",
                          marginBottom: 4,
                        }}
                      >
                        LIFECYCLE
                      </div>

                      <span
                        style={{
                          ...lifecycleStyle,
                          display: "inline-block",
                          fontSize: 11,
                          fontWeight: 500,
                          padding: "2px 8px",
                          borderRadius: 99,
                        }}
                      >
                        {creative.status}
                      </span>
                    </div>

                    {/* Safety */}

                    <div
                      style={{
                        width: 130,
                      }}
                    >
                      <div
                        style={{
                          fontSize: 10,
                          color: "#666",
                          marginBottom: 4,
                        }}
                      >
                        SAFETY
                      </div>

                      <span
                        style={{
                          ...safetyStyle,
                          display: "inline-block",
                          fontSize: 11,
                          fontWeight: 500,
                          padding: "2px 8px",
                          borderRadius: 99,
                        }}
                      >
                        {creative.safetyStatus.replaceAll(
                          "_",
                          " ",
                        )}
                      </span>
                    </div>

                    {/* Lifecycle select */}

                    <select
                      value={creative.status}
                      disabled={actionCreativeId ===
                        creative.id}
                      onChange={(event) =>
                        handleStatusChange(
                          creative,
                          event.target
                            .value as CreativeStatus,
                        )}
                      style={{
                        fontSize: 11,
                        background: "#1a1a1a",
                        color: "#aaa",
                        border: "0.5px solid #2a2a2a",
                        borderRadius: 6,
                        padding: "5px 8px",
                        width: 100,
                      }}
                    >
                      <option value="DRAFT">
                        Draft
                      </option>

                      <option value="ACTIVE">
                        Active
                      </option>

                      <option value="PAUSED">
                        Paused
                      </option>

                      <option value="ARCHIVED">
                        Archived
                      </option>
                    </select>

                    {/* Edit */}

                    <button
                      onClick={() => handleEdit(creative)}
                      disabled={creative.safetyStatus ===
                        "PENDING_REVIEW"}
                      style={{
                        ...secondaryButtonStyle,
                        opacity: creative.safetyStatus ===
                            "PENDING_REVIEW"
                          ? 0.4
                          : 1,
                      }}
                    >
                      Edit
                    </button>

                    {/* Safety action */}

                    {creative.safetyStatus ===
                        "UNREVIEWED"
                      ? (
                        <button
                          onClick={() =>
                            handleSubmitForReview(
                              creative,
                            )}
                          disabled={actionCreativeId ===
                            creative.id}
                          style={{
                            ...secondaryButtonStyle,
                            color: "#e5e5e5",
                            minWidth: 108,
                          }}
                        >
                          Submit review
                        </button>
                      )
                      : (
                        <button
                          onClick={() =>
                            handleToggleReviewPanel(
                              creative.id,
                            )}
                          style={{
                            ...secondaryButtonStyle,
                            minWidth: 108,
                          }}
                        >
                          {expanded ? "Hide review" : creative.safetyStatus ===
                              "PENDING_REVIEW"
                            ? "Review"
                            : "History"}
                        </button>
                      )}
                  </div>

                  {/* Secondary creative details */}

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1.4fr 1fr 1fr",
                      gap: 16,
                      padding: "0 0 12px 0",
                    }}
                  >
                    <div>
                      <div
                        style={{
                          fontSize: 10,
                          color: "#666",
                          marginBottom: 3,
                        }}
                      >
                        CONTENT
                      </div>

                      <div
                        style={{
                          fontSize: 11,
                          color: "#999",
                          lineHeight: 1.45,
                        }}
                      >
                        {creative.contentText}
                      </div>
                    </div>

                    <div>
                      <div
                        style={{
                          fontSize: 10,
                          color: "#666",
                          marginBottom: 3,
                        }}
                      >
                        MEDIA
                      </div>

                      <div
                        style={{
                          fontSize: 11,
                          color: "#888",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                        title={creative.mediaUrl}
                      >
                        {creative.mediaUrl}
                      </div>
                    </div>

                    <div>
                      <div
                        style={{
                          fontSize: 10,
                          color: "#666",
                          marginBottom: 3,
                        }}
                      >
                        DESTINATION
                      </div>

                      <div
                        style={{
                          fontSize: 11,
                          color: "#888",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                        title={creative.destinationUrl}
                      >
                        {creative.destinationUrl}
                      </div>
                    </div>
                  </div>

                  {/* Review panel */}

                  {expanded && (
                    <div
                      style={{
                        background: "#101010",
                        borderTop: "0.5px solid #242424",
                        padding: "14px 16px",
                        margin: "0 -16px 0 -16px",
                      }}
                    >
                      <div
                        style={{
                          fontSize: 12,
                          fontWeight: 500,
                          color: "#e5e5e5",
                          marginBottom: 12,
                        }}
                      >
                        Safety review
                      </div>

                      {creative.safetyStatus ===
                          "PENDING_REVIEW" &&
                        pendingReview && (
                        <div
                          style={{
                            background: "#141414",
                            border: "0.5px solid #2a2a2a",
                            borderRadius: 8,
                            padding: 12,
                            marginBottom: 14,
                          }}
                        >
                          <div
                            style={{
                              fontSize: 11,
                              color: "#fbbf24",
                              fontWeight: 500,
                              marginBottom: 10,
                            }}
                          >
                            Pending human review
                          </div>

                          <div
                            style={{
                              display: "grid",
                              gridTemplateColumns: "1fr 2fr",
                              gap: 10,
                              marginBottom: 10,
                            }}
                          >
                            <input
                              placeholder="Reviewer name"
                              value={reviewer}
                              onChange={(
                                event,
                              ) =>
                                setReviewer(
                                  event.target
                                    .value,
                                )}
                              style={inputStyle}
                            />

                            <input
                              placeholder="Review reason / notes"
                              value={reviewReason}
                              onChange={(
                                event,
                              ) =>
                                setReviewReason(
                                  event.target
                                    .value,
                                )}
                              style={inputStyle}
                            />
                          </div>

                          <div
                            style={{
                              display: "flex",
                              gap: 8,
                            }}
                          >
                            <button
                              onClick={() =>
                                handleReviewDecision(
                                  creative.id,
                                  pendingReview.id,
                                  "APPROVED",
                                )}
                              disabled={actionCreativeId ===
                                creative.id}
                              style={{
                                fontSize: 11,
                                padding: "5px 12px",
                                borderRadius: 6,
                                border: "0.5px solid rgba(74, 222, 128, 0.25)",
                                background: "rgba(74, 222, 128, 0.1)",
                                color: "#4ade80",
                              }}
                            >
                              Approve
                            </button>

                            <button
                              onClick={() =>
                                handleReviewDecision(
                                  creative.id,
                                  pendingReview.id,
                                  "REJECTED",
                                )}
                              disabled={actionCreativeId ===
                                creative.id}
                              style={{
                                fontSize: 11,
                                padding: "5px 12px",
                                borderRadius: 6,
                                border: "0.5px solid rgba(248, 113, 113, 0.25)",
                                background: "rgba(248, 113, 113, 0.1)",
                                color: "#f87171",
                              }}
                            >
                              Reject
                            </button>
                          </div>
                        </div>
                      )}

                      <div
                        style={{
                          fontSize: 11,
                          color: "#777",
                          marginBottom: 8,
                        }}
                      >
                        Review history
                      </div>

                      {reviews.length === 0
                        ? (
                          <div
                            style={{
                              fontSize: 11,
                              color: "#666",
                            }}
                          >
                            No safety reviews yet
                          </div>
                        )
                        : (
                          reviews.map(
                            (review) => {
                              const reviewStyle = review.status ===
                                  "APPROVED"
                                ? {
                                  color: "#4ade80",
                                }
                                : review.status ===
                                    "REJECTED"
                                ? {
                                  color: "#f87171",
                                }
                                : {
                                  color: "#fbbf24",
                                };

                              return (
                                <div
                                  key={review.id}
                                  style={{
                                    display: "grid",
                                    gridTemplateColumns:
                                      "110px 120px 1fr 170px",
                                    gap: 14,
                                    padding: "9px 0",
                                    borderBottom: "0.5px solid #1e1e1e",
                                    alignItems: "center",
                                  }}
                                >
                                  <div
                                    style={{
                                      fontSize: 11,
                                      fontWeight: 500,
                                      ...reviewStyle,
                                    }}
                                  >
                                    {review.status}
                                  </div>

                                  <div
                                    style={{
                                      fontSize: 11,
                                      color: "#888",
                                    }}
                                  >
                                    {review.reviewer ??
                                      "—"}
                                  </div>

                                  <div
                                    style={{
                                      fontSize: 11,
                                      color: "#888",
                                    }}
                                  >
                                    {review.reason ??
                                      "No reason provided"}
                                  </div>

                                  <div
                                    style={{
                                      fontSize: 10,
                                      color: "#666",
                                      textAlign: "right",
                                    }}
                                  >
                                    {formatDate(
                                      review.reviewedAt ??
                                        review.createdAt,
                                    )}
                                  </div>
                                </div>
                              );
                            },
                          )
                        )}
                    </div>
                  )}
                </div>
              );
            })
          )}
      </div>
    </div>
  );
}
