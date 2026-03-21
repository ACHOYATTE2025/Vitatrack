import axios from "axios";
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  removeTokens,
} from "../utils/TokenStorage";

// ── Base URL ───────────────────────────────────────────────────────────────
// Uses VITE_API_URL env variable in production, falls back to local backend
const BASE_URL =
  import.meta.env.VITE_API_URL ||
  "http://localhost:8080/api/vitatrack/v1"; // ← fixed: was stockportefoliotracker

const api = axios.create({
  baseURL: BASE_URL,
});

// ── REQUEST interceptor ────────────────────────────────────────────────────
// Automatically attaches the JWT access token to every non-public request
api.interceptors.request.use((config) => {
  const token = getAccessToken();

  // These routes don't need a token
  const publicRoutes = ["/auth/login", "/auth/register", "/auth/refresh"]; // ← fixed route names
  const isPublicRoute = publicRoutes.some((route) =>
    config.url.endsWith(route)
  );

  if (token && !isPublicRoute) {
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    delete config.headers.Authorization;
  }

  console.log("[API]", config.method.toUpperCase(), config.url);
  return config;
});

// ── RESPONSE interceptor ───────────────────────────────────────────────────
// Handles 401 errors by automatically refreshing the access token and
// replaying all failed requests once a new token is obtained.

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token);
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,

  async (error) => {
    const originalRequest = error.config;

    // Trigger refresh only on 401, not already retried, not the refresh route itself
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url.endsWith("/auth/refresh") // ← fixed route name
    ) {
      // If a refresh is already in progress, queue this request
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const currentRefreshToken = getRefreshToken();
        if (!currentRefreshToken) throw new Error("No refresh token available");

        // Call the VitaTrack refresh endpoint
        const response = await axios.post(`${BASE_URL}/auth/refresh`, { // ← fixed route
          refreshToken: currentRefreshToken,
        });

        // Extract tokens from VitaTrack's ResponseDto structure:
        // { statusCode, statusMsg, data: { accessToken, refreshToken } }
        const newAccessToken = response.data?.data?.accessToken;   // ← fixed: matches your backend
        const newRefreshToken =
          response.data?.data?.refreshToken ?? currentRefreshToken;

        if (!newAccessToken) throw new Error("No access token received from refresh");

        // Save the new token pair
        setTokens(newAccessToken, newRefreshToken);
        console.log("[API] Token refreshed successfully");

        // Replay all queued requests with the new token
        processQueue(null, newAccessToken);

        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);

      } catch (refreshError) {
        console.error("[API] Refresh failed — logging out:", refreshError.message);
        processQueue(refreshError, null);

        // Clear tokens and redirect to login
        removeTokens();
        window.location.href = "/login";

        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    console.error("[API] Error:", error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export default api;