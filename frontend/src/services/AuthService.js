import api from "../api/axiosClient";
import {
  setTokens,
  removeTokens,
  setUsername,
  removeUsername,
  getRefreshToken,
} from "../utils/TokenStorage";

// ── LOGIN ──────────────────────────────────────────────────────────────────

/**
 * Authenticates the user against the VitaTrack backend.
 * Saves the access token, refresh token, and username to localStorage.
 *
 * @param {string} email
 * @param {string} password
 * @returns {object} the full response data
 */
export const login = async (email, password) => {
  const response = await api.post("/auth/login", { email, password });

  // VitaTrack backend returns: { statusCode, statusMsg, data: { accessToken, refreshToken } }
  const { accessToken, refreshToken } = response.data.data;

  if (!accessToken) throw new Error("No access token received from server");

  setTokens(accessToken, refreshToken);
  console.log("[AUTH] Login successful");

  return response.data;
};

// ── REGISTER ───────────────────────────────────────────────────────────────

/**
 * Creates a new user account.
 *
 * @param {string} username
 * @param {string} email
 * @param {string} password
 * @returns {object} the full response data
 */
export const register = async (username, email, password) => {
  const response = await api.post("/auth/register", { username, email, password });
  console.log("[AUTH] Registration successful");
  return response.data;
};

// ── LOGOUT ─────────────────────────────────────────────────────────────────

/**
 * Logs out the user — revokes the refresh token on the backend,
 * then clears all tokens and user data from localStorage.
 */
export const logout = async () => {
  try {
    // Revoke refresh token on the server (removes it from Firestore)
    await api.post("/auth/logout");
    console.log("[AUTH] Server-side logout successful");
  } catch (error) {
    // Even if the server call fails, clear local storage
    console.warn("[AUTH] Server logout failed — clearing local tokens anyway:", error.message);
  } finally {
    removeTokens();
    removeUsername();
    console.log("[AUTH] Local tokens cleared");
  }
};

// ── REFRESH ────────────────────────────────────────────────────────────────

/**
 * Manually requests a new access token using the stored refresh token.
 * Normally called automatically by axiosClient.js on 401 responses.
 * Can also be called directly if needed.
 *
 * @returns {string} the new access token
 */
export const refreshToken = async () => {
  const currentRefreshToken = getRefreshToken();
  if (!currentRefreshToken) throw new Error("No refresh token available");

  const response = await api.post("/auth/refresh", {
    refreshToken: currentRefreshToken,
  });

  const { accessToken, refreshToken: newRefreshToken } = response.data.data;

  if (!accessToken) throw new Error("No access token received from refresh");

  setTokens(accessToken, newRefreshToken);
  console.log("[AUTH] Token refreshed manually");

  return accessToken;
};