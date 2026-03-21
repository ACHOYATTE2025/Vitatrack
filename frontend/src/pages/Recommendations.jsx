import { useState, useEffect } from "react";
import api from "../api/axiosClient";

/**
 * Recommendations page — allows the user to:
 * - Generate a new AI recommendation based on latest health metrics + activities
 * - View past recommendations sorted by most recent first
 */
export default function Recommendations() {
  const [recommendations, setRecommendations] = useState([]);
  const [loading,         setLoading]         = useState(true);
  const [generating,      setGenerating]      = useState(false);
  const [error,           setError]           = useState(null);
  const [success,         setSuccess]         = useState(null);

  // ── Load recommendation history on mount ──────────────────────
  useEffect(() => { fetchRecommendations(); }, []);

  const fetchRecommendations = async () => {
    setLoading(true);
    setError(null);
    try {
      const res  = await api.get("/recommendations");
      const list = res.data?.data ?? res.data ?? [];
      setRecommendations(list);
    } catch (err) {
      setError("Failed to load recommendations.");
    } finally {
      setLoading(false);
    }
  };

  // ── Generate a new recommendation ─────────────────────────────
  const handleGenerate = async () => {
    setGenerating(true);
    setError(null);
    setSuccess(null);
    try {
      const res = await api.post("/recommendations");
      console.log("[RECO] Generated:", res.data);
      setSuccess("New recommendation generated successfully.");
      fetchRecommendations();
    } catch (err) {
      setError(
        err.response?.data?.statusMsg ||
        err.response?.data?.message   ||
        "Failed to generate recommendation. Make sure you have at least one health session recorded."
      );
    } finally {
      setGenerating(false);
    }
  };

  // ── Format ISO date ────────────────────────────────────────────
  const fmtDate = (iso) =>
    iso
      ? new Date(iso).toLocaleDateString("en-US", {
          year:   "numeric",
          month:  "long",
          day:    "numeric",
          hour:   "2-digit",
          minute: "2-digit",
        })
      : "—";

  return (
    <>
      {/* ── Page header ──────────────────────────────────────── */}
      <div className="section-header">
        <div>
          <h2 className="section-title">AI Recommendations</h2>
          <p style={{ fontSize: 14, color: "var(--text-secondary)", marginTop: 4 }}>
            Powered by Groq — llama-3.3-70b-versatile
          </p>
        </div>
        <button
          className="btn btn-success"
          onClick={handleGenerate}
          disabled={generating}
        >
          {generating ? (
            <>
              <span className="btn-spinner" /> Generating...
            </>
          ) : (
            "Generate recommendation"
          )}
        </button>
      </div>

      {error   && <div className="alert alert-error"   style={{ marginBottom: 16 }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: 16 }}>{success}</div>}

      {/* ── Info banner ───────────────────────────────────────── */}
      <div className="reco-info-banner">
        <span className="reco-info-icon">💡</span>
        <p>
          Recommendations are generated from your <strong>3 most recent health sessions</strong> and
          their linked activities. The more data you record, the more personalized the advice.
        </p>
      </div>

      {/* ── Recommendations list ──────────────────────────────── */}
      {loading ? (
        <div className="spinner" />
      ) : recommendations.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🤖</div>
          <p className="empty-state-title">No recommendations yet</p>
          <p className="empty-state-desc">
            Record at least one health session, then click
            <strong> "Generate recommendation"</strong> above.
          </p>
        </div>
      ) : (
        <div>
          {recommendations.map((reco, index) => (
            <div key={reco.id ?? index} className="reco-card">

              {/* Meta info */}
              <div className="reco-card-meta">
                <span className="reco-card-date">
                  📅 {fmtDate(reco.generatedAt)}
                </span>
                <div style={{ display: "flex", gap: 8 }}>
                  {reco.basedOnMetrics && (
                    <span className="badge badge-blue">
                      {reco.basedOnMetrics} session{reco.basedOnMetrics > 1 ? "s" : ""}
                    </span>
                  )}
                  {reco.basedOnActivities > 0 && (
                    <span className="badge badge-green">
                      {reco.basedOnActivities} activit{reco.basedOnActivities > 1 ? "ies" : "y"}
                    </span>
                  )}
                </div>
              </div>

              {/* Recommendation text */}
              <p className="reco-text">{reco.recommendation}</p>

            </div>
          ))}
        </div>
      )}
    </>
  );
}