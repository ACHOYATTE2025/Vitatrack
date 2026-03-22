import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";

export default function LandingPage() {
  const [scrolled, setScrolled] = useState(false);
  const heroRef = useRef(null);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40);
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <div style={styles.root}>
      {/* ── Navbar ── */}
      <nav style={{ ...styles.nav, ...(scrolled ? styles.navScrolled : {}) }}>
        <span style={styles.navLogo}>VitaTrack</span>
        <div style={styles.navLinks}>
          <a href="#features" style={styles.navLink}>Features</a>
          <a href="#how" style={styles.navLink}>How it works</a>
          <Link to="/login"    style={styles.navLinkOutline}>Sign in</Link>
          <Link to="/register" style={styles.navCta}>Get started</Link>
        </div>
      </nav>

      {/* ── Hero ── */}
      <section ref={heroRef} style={styles.hero}>
        <div style={styles.heroBg} aria-hidden="true">
          {[...Array(6)].map((_, i) => (
            <div key={i} style={{ ...styles.heroOrb, ...orbStyles[i] }} />
          ))}
        </div>

        <div style={styles.heroContent}>
          <div style={styles.heroBadge}>
            <span style={styles.heroBadgeDot} />
            AI-powered health tracking
          </div>

          <h1 style={styles.heroTitle}>
            Your health,<br />
            <span style={styles.heroAccent}>understood.</span>
          </h1>

          <p style={styles.heroSub}>
            Track your vitals, log your activities, and receive personalized
            AI recommendations — all in one place, stored securely in the cloud.
          </p>

          <div style={styles.heroCtas}>
            <Link to="/register" style={styles.ctaPrimary}>
              Start for free
              <span style={styles.ctaArrow}>→</span>
            </Link>
            <a href="#features" style={styles.ctaGhost}>See how it works</a>
          </div>

          <div style={styles.heroStats}>
            {[
              { value: "100%", label: "Free to use" },
              { value: "AI", label: "Powered by Groq" },
              { value: "∞", label: "Cloud storage" },
            ].map((s) => (
              <div key={s.label} style={styles.heroStat}>
                <span style={styles.heroStatVal}>{s.value}</span>
                <span style={styles.heroStatLabel}>{s.label}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Floating card mockup */}
        <div style={styles.heroCard}>
          <div style={styles.cardHeader}>
            <span style={styles.cardDot} />
            <span style={styles.cardDot2} />
            <span style={styles.cardDot3} />
          </div>
          <div style={styles.cardMetric}>
            <span style={styles.cardLabel}>BMI</span>
            <span style={styles.cardValue}>22.4</span>
            <span style={styles.cardBadgeNormal}>Normal</span>
          </div>
          <div style={styles.cardDivider} />
          <div style={styles.cardRow}>
            <div style={styles.cardItem}>
              <span style={styles.cardItemLabel}>Heart rate</span>
              <span style={styles.cardItemVal}>68 bpm</span>
            </div>
            <div style={styles.cardItem}>
              <span style={styles.cardItemLabel}>Blood pressure</span>
              <span style={styles.cardItemVal}>120/80</span>
            </div>
          </div>
          <div style={styles.cardDivider} />
          <div style={styles.cardReco}>
            <span style={styles.cardRecoIcon}>✦</span>
            <span style={styles.cardRecoText}>
              AI recommendation ready
            </span>
          </div>
        </div>
      </section>

      {/* ── Features ── */}
      <section id="features" style={styles.features}>
        <div style={styles.sectionInner}>
          <p style={styles.sectionEyebrow}>What you get</p>
          <h2 style={styles.sectionTitle}>Everything you need<br />to track your health</h2>

          <div style={styles.featureGrid}>
            {[
              {
                icon: "◈",
                color: "#0ea5e9",
                bg: "#e0f2fe",
                title: "Health sessions",
                desc: "Record weight, BMI (auto-calculated), heart rate, and blood pressure after each check-in.",
              },
              {
                icon: "⬡",
                color: "#10b981",
                bg: "#d1fae5",
                title: "Activity logging",
                desc: "Link physical activities — running, cycling, swimming — directly to each health session.",
              },
              {
                icon: "◇",
                color: "#8b5cf6",
                bg: "#ede9fe",
                title: "AI recommendations",
                desc: "Groq's llama-3.3-70b analyzes your last 3 sessions and generates 3 actionable tips.",
              },
              {
                icon: "△",
                color: "#f59e0b",
                bg: "#fef3c7",
                title: "Cloud storage",
                desc: "All your data is stored securely in Google Cloud Firestore — accessible anywhere.",
              },
              {
                icon: "○",
                color: "#ef4444",
                bg: "#fee2e2",
                title: "Secure by design",
                desc: "JWT authentication with automatic refresh token rotation keeps your account safe.",
              },
              {
                icon: "▽",
                color: "#0d9488",
                bg: "#ccfbf1",
                title: "REST API",
                desc: "Built on Spring Boot 3 — a production-grade backend you can extend and deploy anywhere.",
              },
            ].map((f) => (
              <div key={f.title} style={styles.featureCard}>
                <div style={{ ...styles.featureIcon, background: f.bg, color: f.color }}>
                  {f.icon}
                </div>
                <h3 style={styles.featureTitle}>{f.title}</h3>
                <p style={styles.featureDesc}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── How it works ── */}
      <section id="how" style={styles.how}>
        <div style={styles.sectionInner}>
          <p style={styles.sectionEyebrow}>Simple by design</p>
          <h2 style={styles.sectionTitle}>Three steps to better health</h2>

          <div style={styles.steps}>
            {[
              {
                n: "01",
                title: "Record a health session",
                desc: "Enter your weight, height, heart rate, and blood pressure. BMI is computed automatically.",
                color: "#0ea5e9",
              },
              {
                n: "02",
                title: "Log your activities",
                desc: "Add running, cycling, or any physical activity to the session. Duration and calories burned.",
                color: "#10b981",
              },
              {
                n: "03",
                title: "Get AI recommendations",
                desc: "Hit Generate — Groq AI analyzes your recent sessions and gives you 3 personalized tips.",
                color: "#8b5cf6",
              },
            ].map((s, i) => (
              <div key={s.n} style={styles.step}>
                <div style={{ ...styles.stepNum, color: s.color }}>{s.n}</div>
                {i < 2 && <div style={styles.stepLine} />}
                <h3 style={styles.stepTitle}>{s.title}</h3>
                <p style={styles.stepDesc}>{s.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA banner ── */}
      <section style={styles.banner}>
        <div style={styles.bannerInner}>
          <h2 style={styles.bannerTitle}>Ready to start tracking?</h2>
          <p style={styles.bannerSub}>
            Create your free account in 30 seconds. No credit card required.
          </p>
          <Link to="/register" style={styles.bannerCta}>
            Create free account →
          </Link>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer style={styles.footer}>
        <div style={styles.footerInner}>
          <span style={styles.footerLogo}>VitaTrack</span>
          <span style={styles.footerCopy}>
            © {new Date().getFullYear()} · Built with Spring Boot & React
          </span>
          <div style={styles.footerLinks}>
            <Link to="/login"    style={styles.footerLink}>Sign in</Link>
            <Link to="/register" style={styles.footerLink}>Register</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}

// ── Orb positions ───────────────────────────────────────────────
const orbStyles = [
  { width: 480, height: 480, top: -100, left: -120, background: "radial-gradient(circle, #bfdbfe 0%, transparent 70%)", animationDelay: "0s" },
  { width: 360, height: 360, top: 80,  right: -80,  background: "radial-gradient(circle, #a7f3d0 0%, transparent 70%)", animationDelay: "1.2s" },
  { width: 280, height: 280, bottom: 40, left: "30%",background: "radial-gradient(circle, #ddd6fe 0%, transparent 70%)", animationDelay: "2.4s" },
  { width: 200, height: 200, top: "40%", left: "10%",background: "radial-gradient(circle, #fde68a 0%, transparent 70%)", animationDelay: "0.6s" },
  { width: 160, height: 160, top: "20%", right: "25%",background: "radial-gradient(circle, #fca5a5 0%, transparent 70%)", animationDelay: "1.8s" },
  { width: 120, height: 120, bottom: 80, right: "15%",background: "radial-gradient(circle, #99f6e4 0%, transparent 70%)", animationDelay: "3s" },
];

// ── Styles ──────────────────────────────────────────────────────
const styles = {
  root: {
    fontFamily: "'DM Sans', 'Outfit', system-ui, sans-serif",
    background: "#fafafa",
    color: "#0f172a",
    minHeight: "100vh",
    overflowX: "hidden",
  },

  // Navbar
  nav: {
    position: "fixed", top: 0, left: 0, right: 0,
    display: "flex", alignItems: "center", justifyContent: "space-between",
    padding: "16px 40px",
    zIndex: 200,
    transition: "background 0.3s, box-shadow 0.3s",
  },
  navScrolled: {
    background: "rgba(250,250,250,0.92)",
    backdropFilter: "blur(12px)",
    boxShadow: "0 1px 0 rgba(0,0,0,0.06)",
  },
  navLogo: {
    fontSize: 22, fontWeight: 800, color: "#0f172a",
    letterSpacing: "-0.5px",
  },
  navLinks: { display: "flex", alignItems: "center", gap: 8 },
  navLink: {
    padding: "8px 14px", borderRadius: 8, fontSize: 14, fontWeight: 500,
    color: "#475569", textDecoration: "none",
    transition: "color 0.15s",
  },
  navLinkOutline: {
    padding: "8px 16px", borderRadius: 8, fontSize: 14, fontWeight: 600,
    color: "#0f172a", textDecoration: "none",
    border: "1px solid #e2e8f0",
    background: "white",
  },
  navCta: {
    padding: "8px 18px", borderRadius: 8, fontSize: 14, fontWeight: 700,
    color: "white", textDecoration: "none",
    background: "#0f172a",
    transition: "opacity 0.15s",
  },

  // Hero
  hero: {
    minHeight: "100vh",
    display: "flex", alignItems: "center", justifyContent: "center",
    padding: "120px 40px 80px",
    position: "relative",
    overflow: "hidden",
    gap: 80,
    flexWrap: "wrap",
  },
  heroBg: {
    position: "absolute", inset: 0, pointerEvents: "none",
  },
  heroOrb: {
    position: "absolute", borderRadius: "50%",
    animation: "pulse 6s ease-in-out infinite",
    opacity: 0.6,
  },
  heroContent: {
    flex: "1 1 480px", maxWidth: 580, zIndex: 1,
  },
  heroBadge: {
    display: "inline-flex", alignItems: "center", gap: 8,
    padding: "6px 14px", borderRadius: 999,
    background: "#f0fdf4", border: "1px solid #bbf7d0",
    fontSize: 13, fontWeight: 600, color: "#15803d",
    marginBottom: 28,
  },
  heroBadgeDot: {
    width: 8, height: 8, borderRadius: "50%",
    background: "#22c55e",
    boxShadow: "0 0 0 3px rgba(34,197,94,0.25)",
    display: "inline-block",
    animation: "pulse 2s ease-in-out infinite",
  },
  heroTitle: {
    fontSize: "clamp(40px, 5vw, 68px)",
    fontWeight: 900,
    lineHeight: 1.08,
    letterSpacing: "-2px",
    color: "#0f172a",
    margin: "0 0 24px",
  },
  heroAccent: {
    background: "linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%)",
    WebkitBackgroundClip: "text",
    WebkitTextFillColor: "transparent",
    backgroundClip: "text",
  },
  heroSub: {
    fontSize: 18, lineHeight: 1.7, color: "#475569",
    margin: "0 0 40px", maxWidth: 460,
  },
  heroCtas: { display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 48 },
  ctaPrimary: {
    display: "inline-flex", alignItems: "center", gap: 8,
    padding: "14px 28px", borderRadius: 12,
    background: "#0f172a", color: "white",
    fontSize: 16, fontWeight: 700, textDecoration: "none",
    transition: "transform 0.15s, box-shadow 0.15s",
    boxShadow: "0 4px 20px rgba(15,23,42,0.25)",
  },
  ctaArrow: { fontSize: 18 },
  ctaGhost: {
    display: "inline-flex", alignItems: "center",
    padding: "14px 24px", borderRadius: 12,
    border: "1.5px solid #e2e8f0", color: "#475569",
    fontSize: 16, fontWeight: 600, textDecoration: "none",
    background: "white",
  },
  heroStats: { display: "flex", gap: 32 },
  heroStat: { display: "flex", flexDirection: "column", gap: 2 },
  heroStatVal: { fontSize: 28, fontWeight: 800, color: "#0f172a", letterSpacing: "-1px" },
  heroStatLabel: { fontSize: 13, color: "#94a3b8", fontWeight: 500 },

  // Floating card
  heroCard: {
    flex: "0 0 300px",
    background: "white",
    borderRadius: 20,
    padding: "24px",
    boxShadow: "0 20px 60px rgba(15,23,42,0.12), 0 4px 16px rgba(15,23,42,0.06)",
    border: "1px solid #f1f5f9",
    zIndex: 1,
    animation: "float 4s ease-in-out infinite",
  },
  cardHeader: { display: "flex", gap: 6, marginBottom: 20 },
  cardDot:  { width: 10, height: 10, borderRadius: "50%", background: "#fca5a5" },
  cardDot2: { width: 10, height: 10, borderRadius: "50%", background: "#fde68a" },
  cardDot3: { width: 10, height: 10, borderRadius: "50%", background: "#a7f3d0" },
  cardMetric: { display: "flex", alignItems: "center", gap: 12, marginBottom: 16 },
  cardLabel: { fontSize: 12, fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.08em" },
  cardValue: { fontSize: 36, fontWeight: 900, color: "#0f172a", letterSpacing: "-1.5px", flex: 1 },
  cardBadgeNormal: {
    padding: "4px 10px", borderRadius: 999,
    background: "#dcfce7", color: "#15803d",
    fontSize: 12, fontWeight: 700,
  },
  cardDivider: { height: 1, background: "#f1f5f9", margin: "16px 0" },
  cardRow: { display: "flex", gap: 16, marginBottom: 0 },
  cardItem: { flex: 1, display: "flex", flexDirection: "column", gap: 4 },
  cardItemLabel: { fontSize: 11, color: "#94a3b8", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" },
  cardItemVal: { fontSize: 16, fontWeight: 700, color: "#0f172a" },
  cardReco: {
    display: "flex", alignItems: "center", gap: 8,
    padding: "10px 14px", borderRadius: 10,
    background: "linear-gradient(135deg, #ede9fe, #dbeafe)",
    marginTop: 16,
  },
  cardRecoIcon: { color: "#7c3aed", fontSize: 14, fontWeight: 700 },
  cardRecoText: { fontSize: 13, fontWeight: 600, color: "#4c1d95" },

  // Features
  features: {
    padding: "100px 40px",
    background: "white",
  },
  sectionInner: { maxWidth: 1100, margin: "0 auto" },
  sectionEyebrow: {
    fontSize: 13, fontWeight: 700, color: "#0ea5e9",
    textTransform: "uppercase", letterSpacing: "0.1em",
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: "clamp(28px, 3.5vw, 44px)",
    fontWeight: 800, letterSpacing: "-1px",
    color: "#0f172a", marginBottom: 56, lineHeight: 1.15,
  },
  featureGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))",
    gap: 24,
  },
  featureCard: {
    padding: "28px 24px",
    borderRadius: 16,
    border: "1px solid #f1f5f9",
    background: "#fafafa",
    transition: "transform 0.15s, box-shadow 0.15s",
  },
  featureIcon: {
    width: 44, height: 44, borderRadius: 10,
    display: "flex", alignItems: "center", justifyContent: "center",
    fontSize: 20, marginBottom: 16, fontWeight: 900,
  },
  featureTitle: { fontSize: 17, fontWeight: 700, color: "#0f172a", marginBottom: 8 },
  featureDesc:  { fontSize: 14, color: "#64748b", lineHeight: 1.7 },

  // How it works
  how: {
    padding: "100px 40px",
    background: "#f8fafc",
  },
  steps: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
    gap: 40, marginTop: 8,
    position: "relative",
  },
  step: { position: "relative" },
  stepNum: {
    fontSize: 56, fontWeight: 900, lineHeight: 1,
    letterSpacing: "-2px", marginBottom: 16,
    fontVariantNumeric: "tabular-nums",
  },
  stepLine: {
    display: "none",
  },
  stepTitle: { fontSize: 20, fontWeight: 700, color: "#0f172a", marginBottom: 10 },
  stepDesc:  { fontSize: 15, color: "#475569", lineHeight: 1.7 },

  // CTA Banner
  banner: {
    padding: "100px 40px",
    background: "#0f172a",
    textAlign: "center",
  },
  bannerInner: { maxWidth: 600, margin: "0 auto" },
  bannerTitle: {
    fontSize: "clamp(28px, 4vw, 44px)",
    fontWeight: 900, color: "white",
    letterSpacing: "-1.5px", marginBottom: 16,
  },
  bannerSub: { fontSize: 17, color: "#94a3b8", marginBottom: 40, lineHeight: 1.6 },
  bannerCta: {
    display: "inline-block",
    padding: "16px 36px", borderRadius: 12,
    background: "white", color: "#0f172a",
    fontSize: 16, fontWeight: 700, textDecoration: "none",
    transition: "transform 0.15s",
    boxShadow: "0 4px 20px rgba(255,255,255,0.15)",
  },

  // Footer
  footer: {
    background: "#0f172a",
    borderTop: "1px solid rgba(255,255,255,0.06)",
    padding: "24px 40px",
  },
  footerInner: {
    maxWidth: 1100, margin: "0 auto",
    display: "flex", alignItems: "center", justifyContent: "space-between",
    flexWrap: "wrap", gap: 12,
  },
  footerLogo: { fontSize: 16, fontWeight: 800, color: "white" },
  footerCopy: { fontSize: 13, color: "#475569" },
  footerLinks: { display: "flex", gap: 20 },
  footerLink: { fontSize: 13, color: "#64748b", textDecoration: "none", fontWeight: 500 },
};