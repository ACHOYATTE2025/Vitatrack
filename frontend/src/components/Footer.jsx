import { Link } from "react-router-dom";

/**
 * Footer component — displayed at the bottom of every protected page.
 * Shows app name, navigation links, and current year.
 */
export default function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className="footer">
      <div className="footer-inner">

        {/* Brand */}
        <div className="footer-brand">
          <span className="footer-logo">VitaTrack</span>
          <p className="footer-tagline">
            Your personal health and fitness tracker
          </p>
        </div>

        {/* Nav links */}
        <div className="footer-nav">
          <Link to="/dashboard"       className="footer-link">Dashboard</Link>
          <Link to="/health"          className="footer-link">Health Metrics</Link>
          <Link to="/recommendations" className="footer-link">AI Recommendations</Link>
        </div>

        {/* Copyright */}
        <div className="footer-copy">
          © {year} VitaTrack — Built with Spring Boot & React
        </div>

      </div>
    </footer>
  );
}