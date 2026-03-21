import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { logout } from "../services/AuthService";

export default function Navbar() {
  const navigate  = useNavigate();
  const location  = useLocation();
  const [open, setOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const isActive = (path) =>
    location.pathname === path ? "nav-link active" : "nav-link";

  return (
    <>
      <nav className="navbar">
        <Link to="/dashboard" className="navbar-brand">VitaTrack</Link>

        {/* Desktop nav */}
        <div className="navbar-nav">
          <Link to="/dashboard"       className={isActive("/dashboard")}>Dashboard</Link>
          <Link to="/health"          className={isActive("/health")}>Health</Link>
          <Link to="/recommendations" className={isActive("/recommendations")}>AI Reco</Link>
          <button onClick={handleLogout} className="btn btn-outline" style={{ fontSize: 13, padding: "6px 14px" }}>
            Sign out
          </button>
        </div>

        {/* Mobile hamburger */}
        <button
          className="navbar-toggle"
          onClick={() => setOpen(!open)}
          aria-label="Toggle menu"
        >
          <span />
          <span />
          <span />
        </button>
      </nav>

      {/* Mobile drawer */}
      <div className={`navbar-drawer ${open ? "open" : ""}`}>
        <Link to="/dashboard"       className={isActive("/dashboard")}       onClick={() => setOpen(false)}>Dashboard</Link>
        <Link to="/health"          className={isActive("/health")}          onClick={() => setOpen(false)}>Health</Link>
        <Link to="/recommendations" className={isActive("/recommendations")} onClick={() => setOpen(false)}>AI Reco</Link>
        <button onClick={handleLogout} className="btn btn-outline btn-full" style={{ marginTop: 8 }}>
          Sign out
        </button>
      </div>
    </>
  );
}