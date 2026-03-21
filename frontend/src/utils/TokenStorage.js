const ACCESS_TOKEN_KEY  = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";
const USERNAME_KEY      = "username";

// ── Token management ───────────────────────────────────────────────────────

/** Saves both tokens to localStorage after login or refresh */
export const setTokens = (accessToken, refreshToken) => {
  localStorage.setItem(ACCESS_TOKEN_KEY,  accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
};

/** Returns the current access token (short-lived, 15 min) */
export const getAccessToken  = () => localStorage.getItem(ACCESS_TOKEN_KEY);

/** Returns the current refresh token (long-lived, 7 days) */
export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY);

/** Removes both tokens — called on logout or when refresh fails */
export const removeTokens = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
};

// ── User info ──────────────────────────────────────────────────────────────

/** Saves the username displayed in the UI after login */
export const setUsername    = (username) => localStorage.setItem(USERNAME_KEY, username);

/** Returns the stored username */
export const getUsername    = () => localStorage.getItem(USERNAME_KEY);

/** Removes the username — called on logout */
export const removeUsername = () => localStorage.removeItem(USERNAME_KEY);

// ── Auth check ─────────────────────────────────────────────────────────────

/**
 * Returns true if the user has a valid access token in storage.
 * Used to protect routes — redirect to /login if false.
 */
export const isAuthenticated = () => !!getAccessToken();