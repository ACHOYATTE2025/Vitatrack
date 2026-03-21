import { useState, useEffect } from "react";
import api from "../api/axiosClient";

/**
 * HealthMetrics page — allows the user to:
 * - Record a new health metric session (weight, BMI auto-computed, heart rate, blood pressure)
 * - Log physical activities under a specific session (subcollection)
 * - View all past health sessions with their linked activities
 */
export default function HealthMetrics() {
  const [metrics,    setMetrics]    = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState(null);
  const [success,    setSuccess]    = useState(null);

  // ── Metric form state ────────────────────────────────────────
  const [metricForm, setMetricForm] = useState({
    weightKg:  "",
    heightCm:  "",
    heartRate: "",
    systolic:  "",
    diastolic: "",
    notes:     "",
  });
  const [metricLoading, setMetricLoading] = useState(false);

  // ── Activity form state ──────────────────────────────────────
  const [activityForm, setActivityForm] = useState({
    type:            "",
    durationMinutes: "",
    caloriesBurned:  "",
    notes:           "",
  });
  const [selectedMetricId, setSelectedMetricId] = useState(null);
  const [activityLoading,  setActivityLoading]  = useState(false);

  // ── Expanded rows ────────────────────────────────────────────
  const [expandedId, setExpandedId] = useState(null);
  const [activities, setActivities] = useState({}); // { metricId: [...] }

  // ── Load metrics on mount ────────────────────────────────────
  useEffect(() => { fetchMetrics(); }, []);

  const fetchMetrics = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get("/health");
      const list = res.data?.data ?? res.data ?? [];
      // Sort most recent first
      list.sort((a, b) =>
        new Date(b.recordedAt) - new Date(a.recordedAt)
      );
      setMetrics(list);
    } catch (err) {
      setError("Failed to load health metrics.");
    } finally {
      setLoading(false);
    }
  };

  // ── Submit new metric ────────────────────────────────────────
  const handleMetricSubmit = async (e) => {
    e.preventDefault();
    setMetricLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await api.post("/health", {
        weightKg:  metricForm.weightKg  ? parseFloat(metricForm.weightKg)  : null,
        heightCm:  metricForm.heightCm  ? parseFloat(metricForm.heightCm)  : null,
        heartRate: metricForm.heartRate ? parseInt(metricForm.heartRate)   : null,
        systolic:  metricForm.systolic  ? parseInt(metricForm.systolic)    : null,
        diastolic: metricForm.diastolic ? parseInt(metricForm.diastolic)   : null,
        notes:     metricForm.notes     || null,
      });
      setSuccess("Health session recorded successfully.");
      setMetricForm({ weightKg: "", heightCm: "", heartRate: "", systolic: "", diastolic: "", notes: "" });
      fetchMetrics();
    } catch (err) {
      setError(err.response?.data?.statusMsg || "Failed to record health session.");
    } finally {
      setMetricLoading(false);
    }
  };

  // ── Load activities for a metric ─────────────────────────────
  const toggleExpand = async (metricId) => {
    if (expandedId === metricId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(metricId);
    if (!activities[metricId]) {
      try {
        const res = await api.get(`/health/${metricId}/activities`);
        const list = res.data?.data ?? res.data ?? [];
        setActivities((prev) => ({ ...prev, [metricId]: list }));
      } catch {
        setActivities((prev) => ({ ...prev, [metricId]: [] }));
      }
    }
  };

  // ── Submit new activity ───────────────────────────────────────
  const handleActivitySubmit = async (e) => {
    e.preventDefault();
    if (!selectedMetricId) return;
    setActivityLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await api.post(`/health/${selectedMetricId}/activities`, {
        type:            activityForm.type,
        durationMinutes: parseInt(activityForm.durationMinutes),
        caloriesBurned:  parseFloat(activityForm.caloriesBurned),
        notes:           activityForm.notes || null,
      });
      setSuccess("Activity logged successfully.");
      setActivityForm({ type: "", durationMinutes: "", caloriesBurned: "", notes: "" });
      // Refresh activities for this metric
      const res = await api.get(`/health/${selectedMetricId}/activities`);
      const list = res.data?.data ?? res.data ?? [];
      setActivities((prev) => ({ ...prev, [selectedMetricId]: list }));
      setSelectedMetricId(null);
    } catch (err) {
      setError(err.response?.data?.statusMsg || "Failed to log activity.");
    } finally {
      setActivityLoading(false);
    }
  };

  // ── Delete metric ─────────────────────────────────────────────
  const handleDeleteMetric = async (metricId) => {
    if (!window.confirm("Delete this health session?")) return;
    try {
      await api.delete(`/health/${metricId}`);
      setMetrics((prev) => prev.filter((m) => m.id !== metricId));
      setSuccess("Session deleted.");
    } catch {
      setError("Failed to delete session.");
    }
  };

  // ── BMI label ─────────────────────────────────────────────────
  const bmiLabel = (bmi) => {
    if (!bmi) return null;
    if (bmi < 18.5) return <span className="badge badge-blue">Underweight</span>;
    if (bmi < 25)   return <span className="badge badge-green">Normal</span>;
    if (bmi < 30)   return <span className="badge badge-orange">Overweight</span>;
    return               <span className="badge badge-red">Obese</span>;
  };

  const fmt = (val, unit = "") => val != null ? `${val}${unit}` : "—";
  const fmtDate = (iso) => iso ? new Date(iso).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" }) : "—";

  return (
    <>
      {/* ── Page header ──────────────────────────────────────── */}
      <div className="section-header">
        <h2 className="section-title">Health Metrics</h2>
      </div>

      {error   && <div className="alert alert-error"   style={{ marginBottom: 16 }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: 16 }}>{success}</div>}

      {/* ── Record new session form ───────────────────────────── */}
      <div className="card" style={{ marginBottom: 32 }}>
        <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 20, color: "var(--text-primary)" }}>
          Record a new health session
        </h3>
        <form onSubmit={handleMetricSubmit}>
          <div style={grid2}>
            <div className="form-group">
              <label className="form-label">Weight (kg)</label>
              <input className="form-input" type="number" step="0.1" placeholder="75.5"
                value={metricForm.weightKg}
                onChange={(e) => setMetricForm({ ...metricForm, weightKg: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">Height (cm)</label>
              <input className="form-input" type="number" step="0.1" placeholder="178"
                value={metricForm.heightCm}
                onChange={(e) => setMetricForm({ ...metricForm, heightCm: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">Heart rate (bpm)</label>
              <input className="form-input" type="number" placeholder="68"
                value={metricForm.heartRate}
                onChange={(e) => setMetricForm({ ...metricForm, heartRate: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">Blood pressure (mmHg)</label>
              <div style={{ display: "flex", gap: 8 }}>
                <input className="form-input" type="number" placeholder="Systolic (120)"
                  value={metricForm.systolic}
                  onChange={(e) => setMetricForm({ ...metricForm, systolic: e.target.value })} />
                <input className="form-input" type="number" placeholder="Diastolic (80)"
                  value={metricForm.diastolic}
                  onChange={(e) => setMetricForm({ ...metricForm, diastolic: e.target.value })} />
              </div>
            </div>
          </div>
          <div className="form-group" style={{ marginTop: 12 }}>
            <label className="form-label">Notes (optional)</label>
            <input className="form-input" type="text" placeholder="e.g. After morning run"
              value={metricForm.notes}
              onChange={(e) => setMetricForm({ ...metricForm, notes: e.target.value })} />
          </div>
          <div style={{ marginTop: 16, textAlign: "right" }}>
            <button type="submit" className="btn btn-primary" disabled={metricLoading}>
              {metricLoading ? "Saving..." : "Save session"}
            </button>
          </div>
        </form>
      </div>

      {/* ── Log activity form (shown when a metric is selected) ── */}
      {selectedMetricId && (
        <div className="card" style={{ marginBottom: 32, borderLeft: "4px solid var(--success)" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
            <h3 style={{ fontSize: 16, fontWeight: 600, color: "var(--text-primary)" }}>
              Log activity for this session
            </h3>
            <button className="btn btn-outline" style={{ fontSize: 12, padding: "4px 10px" }}
              onClick={() => setSelectedMetricId(null)}>
              Cancel
            </button>
          </div>
          <form onSubmit={handleActivitySubmit}>
            <div style={grid2}>
              <div className="form-group">
                <label className="form-label">Activity type</label>
                <select className="form-input" required
                  value={activityForm.type}
                  onChange={(e) => setActivityForm({ ...activityForm, type: e.target.value })}>
                  <option value="">Select type</option>
                  <option>Running</option>
                  <option>Cycling</option>
                  <option>Swimming</option>
                  <option>Walking</option>
                  <option>Gym</option>
                  <option>Yoga</option>
                  <option>Other</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Duration (min)</label>
                <input className="form-input" type="number" min="1" placeholder="45" required
                  value={activityForm.durationMinutes}
                  onChange={(e) => setActivityForm({ ...activityForm, durationMinutes: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Calories burned</label>
                <input className="form-input" type="number" min="0" step="0.1" placeholder="380" required
                  value={activityForm.caloriesBurned}
                  onChange={(e) => setActivityForm({ ...activityForm, caloriesBurned: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Notes (optional)</label>
                <input className="form-input" type="text" placeholder="e.g. Morning run in the park"
                  value={activityForm.notes}
                  onChange={(e) => setActivityForm({ ...activityForm, notes: e.target.value })} />
              </div>
            </div>
            <div style={{ marginTop: 16, textAlign: "right" }}>
              <button type="submit" className="btn btn-success" disabled={activityLoading}>
                {activityLoading ? "Saving..." : "Log activity"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ── Sessions list ─────────────────────────────────────── */}
      <div className="card">
        <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 20, color: "var(--text-primary)" }}>
          Health sessions
        </h3>

        {loading ? (
          <div className="spinner" />
        ) : metrics.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📋</div>
            <p className="empty-state-title">No sessions yet</p>
            <p className="empty-state-desc">Record your first health session above.</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Weight</th>
                  <th>BMI</th>
                  <th>Heart rate</th>
                  <th>Blood pressure</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {metrics.map((m) => (
                  <>
                    <tr key={m.id}>
                      <td>{fmtDate(m.recordedAt)}</td>
                      <td>{fmt(m.weightKg, " kg")}</td>
                      <td>
                        {m.bmi ? (
                          <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
                            {m.bmi} {bmiLabel(m.bmi)}
                          </span>
                        ) : "—"}
                      </td>
                      <td>{fmt(m.heartRate, " bpm")}</td>
                      <td>
                        {m.systolic && m.diastolic
                          ? `${m.systolic}/${m.diastolic} mmHg`
                          : "—"}
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                          <button
                            className="btn btn-outline"
                            style={{ fontSize: 12, padding: "4px 10px" }}
                            onClick={() => toggleExpand(m.id)}>
                            {expandedId === m.id ? "Hide" : "Activities"}
                          </button>
                          <button
                            className="btn btn-success"
                            style={{ fontSize: 12, padding: "4px 10px" }}
                            onClick={() => {
                              setSelectedMetricId(m.id);
                              setExpandedId(m.id);
                            }}>
                            + Add
                          </button>
                          <button
                            className="btn btn-danger"
                            style={{ fontSize: 12, padding: "4px 10px" }}
                            onClick={() => handleDeleteMetric(m.id)}>
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>

                    {/* ── Expanded activities row ──────────────── */}
                    {expandedId === m.id && (
                      <tr key={`${m.id}-activities`}>
                        <td colSpan={6} style={{ background: "#f8fafc", padding: "12px 16px" }}>
                          {!activities[m.id] ? (
                            <div className="spinner" style={{ margin: "12px auto" }} />
                          ) : activities[m.id].length === 0 ? (
                            <p style={{ fontSize: 13, color: "var(--text-muted)", textAlign: "center", padding: "8px 0" }}>
                              No activities logged for this session yet.
                            </p>
                          ) : (
                            <table style={{ margin: 0 }}>
                              <thead>
                                <tr>
                                  <th>Type</th>
                                  <th>Duration</th>
                                  <th>Calories</th>
                                  <th>Notes</th>
                                </tr>
                              </thead>
                              <tbody>
                                {activities[m.id].map((a) => (
                                  <tr key={a.id}>
                                    <td><span className="badge badge-blue">{a.type}</span></td>
                                    <td>{fmt(a.durationMinutes, " min")}</td>
                                    <td>{fmt(a.caloriesBurned, " kcal")}</td>
                                    <td style={{ color: "var(--text-secondary)" }}>{a.notes || "—"}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          )}
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}

// ── Shared style — 2-column responsive grid ────────────────────
const grid2 = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
  gap: 16,
};