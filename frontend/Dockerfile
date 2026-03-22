# ── Stage 1: Build ─────────────────────────────────────────────
# Use node:20-slim (Debian-based) — NOT alpine
# Vite 6 uses rolldown which requires glibc (musl = Alpine breaks it)
FROM node:20-slim AS build

WORKDIR /app

# Copy package files first — caches node_modules layer separately
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copy source and build
COPY . .

# VITE_API_URL injected at build time
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ── Stage 2: Serve ─────────────────────────────────────────────
# Nginx Alpine is fine for serving static files — no native bindings needed
FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/*

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]