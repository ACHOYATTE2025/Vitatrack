import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { isAuthenticated } from "./utils/TokenStorage";
import Layout from "./components/Layout";

import Login           from "./Auth/Login";
import Register        from "./Auth/Register";
import Dashboard       from "./pages/Dashboard";
import HealthMetrics   from "./pages/HealthMetrics";
import Recommendations from "./pages/Recommendations";

// Redirects to /login if no token in localStorage
const ProtectedRoute = ({ children }) => {
  return isAuthenticated()
    ? <Layout>{children}</Layout>
    : <Navigate to="/login" replace />;
};

export default function App() {
  return (
    <BrowserRouter>
      <Routes>

        {/* Public routes — no layout */}
        <Route path="/login"    element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Protected routes — wrapped in Layout automatically */}
        <Route path="/dashboard" element={
          <ProtectedRoute><Dashboard /></ProtectedRoute>
        } />
        <Route path="/health" element={
          <ProtectedRoute><HealthMetrics /></ProtectedRoute>
        } />
        <Route path="/recommendations" element={
          <ProtectedRoute><Recommendations /></ProtectedRoute>
        } />

        {/* Default redirect */}
        <Route path="*" element={
          <Navigate to={isAuthenticated() ? "/dashboard" : "/login"} replace />
        } />

      </Routes>
    </BrowserRouter>
  );
}