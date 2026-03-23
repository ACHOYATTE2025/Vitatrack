# 🏃 VitaTrack — Health & Fitness Tracker

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=flat-square&logo=vite)](https://vitejs.dev/)
[![Firestore](https://img.shields.io/badge/Firestore-Cloud-FFCA28?style=flat-square&logo=firebase)](https://firebase.google.com/docs/firestore)
[![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens)](https://jwt.io/)
[![Docker](https://img.shields.io/badge/Docker-Hub-2496ED?style=flat-square&logo=docker)](https://hub.docker.com/u/achoyatte2025)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

> **VitaTrack** is a cloud-native full-stack application for tracking physical activities and health metrics. Built with Spring Boot 3, secured with JWT, powered by Google Cloud Firestore, and fully containerized with Docker.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Docker Hub — Quick Deploy](#-docker-hub--quick-deploy)
- [Local Setup (from source)](#-local-setup-from-source)
- [Environment Variables](#-environment-variables)
- [API Reference](#-api-reference)
- [Request & Response Examples](#-request--response-examples)
- [Firestore Data Structure](#-firestore-data-structure)
- [Security](#-security)
- [Deployment](#-deployment)
- [Author](#-author)

---

## 🌟 Overview

VitaTrack lets users register, log physical activities (running, cycling, swimming, etc.), record health metrics (weight, BMI, heart rate, blood pressure), and retrieve their history securely from any device.

**Key design decisions:**

- No relational database — all data lives in **Firestore** collections (serverless, scalable)
- Stateless authentication via **JWT** — no sessions, no cookies
- **Ownership enforcement** on every request — users can only access their own data
- **BMI is auto-computed** from weight and height on every health metric entry
- **Fully Dockerized** — run the entire stack with one command via Docker Compose

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Client Browser                   │
└────────────────────┬────────────────────────────────┘
                     │ HTTP / REST
┌────────────────────▼────────────────────────────────┐
│             Frontend (React + Vite)                 │
│                  Port 80 (nginx)                    │
│  Proxies /api/* → backend:8080                      │
└────────────────────┬────────────────────────────────┘
                     │ Internal Docker network
┌────────────────────▼────────────────────────────────┐
│           Backend (Spring Boot 3)                   │
│                  Port 8080                          │
│  Spring Security · JWT Filter · REST Controllers   │
└────────────────────┬────────────────────────────────┘
                     │ Firebase Admin SDK
┌────────────────────▼────────────────────────────────┐
│          Google Cloud Firestore                     │
│   users/ · activities/ · health_metrics/            │
└─────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.2.5 |
| Security | Spring Security + JWT (jjwt) | 0.12.3 |
| Database | Google Cloud Firestore (Firebase Admin SDK) | 9.2.0 |
| Password hashing | BCrypt | — |
| Build tool | Maven | 3.8+ |
| Frontend | React + Vite | 18 / 5 |
| Web server | Nginx | Alpine |
| Containerization | Docker + Docker Compose | — |
| Registry | Docker Hub | — |
| Backend hosting | Railway | — |
| Frontend hosting | Vercel | — |

---

## 📁 Project Structure

```
vitatrack/
├── backend/
│   ├── Dockerfile                          # Multi-stage build (Maven → JRE 17)
│   └── src/main/java/com/portfolio/VitaTrack/
│       ├── Config/
│       │   ├── FirebaseConfig.java         # Firebase Admin SDK initialization
│       │   └── SecurityConfig.java         # Spring Security + CORS configuration
│       ├── Controllers/
│       │   ├── AuthController.java         # POST /register, POST /login
│       │   ├── ActivityController.java     # CRUD /activity
│       │   └── HealthMetricController.java # CRUD /health
│       ├── Services/
│       │   ├── FirestoreService.java       # Generic Firestore CRUD wrapper
│       │   ├── JwtService.java             # Token generation + validation
│       │   ├── AuthService.java            # Register + login logic
│       │   ├── ActivityService.java        # Activity business logic
│       │   └── HealthMetricService.java    # Health metric business logic
│       ├── Security/
│       │   └── JwtAuthFilter.java          # JWT filter (OncePerRequestFilter)
│       ├── Dto/
│       │   ├── SignupRequestDto.java
│       │   ├── LoginRequestDto.java
│       │   ├── ActivityRequestDto.java
│       │   ├── HealthMetricRequestDto.java
│       │   └── ResponseDto.java
│       └── Exception/
│           ├── ApplicationAdvice.java      # Global exception handler
│           └── ResourceNotFoundException.java
│
├── frontend/
│   ├── Dockerfile                          # Multi-stage build (Node → nginx)
│   └── src/                               # React + Vite source
│
├── docker-compose.yml                      # Local development stack
├── docker-compose.prod.yml                 # Production stack (Docker Hub images)
├── env.example                             # Environment variable template
└── README.md
```

---

## 🐳 Docker Hub — Quick Deploy

The fastest way to run VitaTrack is to pull the pre-built images directly from Docker Hub — **no build step required**.

### Images

| Service | Docker Hub Image |
|---|---|
| Backend | `achoyatte2025/vitatrack-backend:latest` |
| Frontend | `achoyatte2025/vitatrack-frontend:latest` |

### Pull the images manually

```bash
docker pull achoyatte2025/vitatrack-backend:latest
docker pull achoyatte2025/vitatrack-frontend:latest
```

### One-command deployment with Docker Hub images

**1. Create your `.env` file:**

```bash
cp env.example .env
# Then fill in your values (see Environment Variables section)
```

**2. Use `docker-compose.prod.yml` which references Docker Hub images directly:**

```yaml
# docker-compose.prod.yml
version: "3.9"

services:
  backend:
    image: achoyatte2025/vitatrack-backend:latest
    container_name: vitatrack-backend
    ports:
      - "8080:8080"
    environment:
      - JWT_KEY=${JWT_KEY}
      - FIREBASE_CREDENTIALS=${FIREBASE_CREDENTIALS}
      - GROQ_API_KEY=${GROQ_API_KEY}
    dns:
      - 8.8.8.8
      - 8.8.4.4
      - 1.1.1.1
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/api/vitatrack/v1/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  frontend:
    image: achoyatte2025/vitatrack-frontend:latest
    container_name: vitatrack-frontend
    ports:
      - "80:80"
    dns:
      - 8.8.8.8
      - 8.8.4.4
    depends_on:
      backend:
        condition: service_healthy
    restart: unless-stopped
```

**3. Start the stack:**

```bash
docker compose -f docker-compose.prod.yml up -d
```

**4. Verify the services are running:**

```bash
docker compose -f docker-compose.prod.yml ps
docker logs vitatrack-backend --tail 50
```

**5. Access the app:**

| Service | URL |
|---|---|
| Frontend | http://localhost |
| Backend API | http://localhost:8080/api/vitatrack/v1 |
| Health check | http://localhost:8080/api/vitatrack/v1/actuator/health |

### Stop the stack

```bash
docker compose -f docker-compose.prod.yml down
```

---

## ⚙️ Local Setup (from source)

Use this approach if you want to modify the code and rebuild.

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- Docker & Docker Compose
- A Firebase project with **Firestore enabled**
- `serviceAccountKey.json` from Firebase Console → Project Settings → Service Accounts

### Option A — Docker Compose (recommended)

```bash
# Clone the repository
git clone https://github.com/ACHOYATTE2025/Vitatrack.git
cd Vitatrack

# Configure environment variables
cp env.example .env
# Fill in JWT_KEY, FIREBASE_CREDENTIALS, and GROQ_API_KEY in .env

# Build and start all services
docker compose up --build -d

# Follow logs
docker compose logs -f
```

App available at: **http://localhost**

### Option B — Run services individually

**Backend:**

```bash
cd backend

# Place serviceAccountKey.json in src/main/resources/

# Export environment variables (Linux/macOS)
export JWT_KEY="your-256-bit-minimum-secret-key"
export GROQ_API_KEY="your-groq-api-key"

# Windows PowerShell
$env:JWT_KEY="your-256-bit-minimum-secret-key"

# Start
mvn spring-boot:run
# API: http://localhost:8080/api/vitatrack/v1
```

**Frontend:**

```bash
cd frontend
npm install

# Create .env.local
echo "VITE_API_URL=http://localhost:8080/api/vitatrack/v1" > .env.local

npm run dev
# App: http://localhost:5173
```

---

## 🔐 Environment Variables

| Variable | Required | Description |
|---|---|---|
| `JWT_KEY` | ✅ Always | Secret key for signing JWT tokens (minimum 256 bits) |
| `FIREBASE_CREDENTIALS` | ✅ Production | Full JSON content of `serviceAccountKey.json` |
| `GROQ_API_KEY` | ✅ Always | API key for Groq AI integration |

**Local development:** place `serviceAccountKey.json` in `backend/src/main/resources/` — the app loads it automatically.

**Production (Railway / Docker):** paste the entire JSON content of `serviceAccountKey.json` as the value of `FIREBASE_CREDENTIALS`.

> ⚠️ Never commit `serviceAccountKey.json` or your `.env` file to Git. Both are already listed in `.gitignore`.

**`env.example` template:**

```env
JWT_KEY=vitatrack-secret-key-256-bits-minimum-length-required-here
FIREBASE_CREDENTIALS={"type":"service_account","project_id":"your-project",...}
GROQ_API_KEY=your-groq-api-key-here
```

---

## 📡 API Reference

**Base URL:** `/api/vitatrack/v1`

All protected routes require:
```
Authorization: Bearer <jwt_token>
```

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | Public | Create a new user account |
| `POST` | `/auth/login` | Public | Authenticate and receive a JWT token |

### Activity Tracking

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/activity` | 🔒 JWT | Log a new physical activity |
| `GET` | `/activity` | 🔒 JWT | Get all activities for the current user |
| `GET` | `/activity/{id}` | 🔒 JWT | Get a single activity by ID |
| `DELETE` | `/activity/{id}` | 🔒 JWT | Delete an activity |

### Health Metrics

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/health` | 🔒 JWT | Record a new health metric entry |
| `GET` | `/health` | 🔒 JWT | Get all health metrics for the current user |
| `GET` | `/health/{id}` | 🔒 JWT | Get a single metric by ID |
| `DELETE` | `/health/{id}` | 🔒 JWT | Delete a health metric |

### System

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | Public | Application health check (used by Docker) |

---

## 📨 Request & Response Examples

### `POST /auth/register`

```json
// Request
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "mypassword123"
}

// Response 201
{
  "status": 201,
  "message": "Account created successfully for john@example.com",
  "data": ""
}
```

### `POST /auth/login`

```json
// Response 200
{
  "status": 200,
  "message": "Login successful",
  "data": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLWlkIn0..."
}
```

> Use the token from `data` as a Bearer token on all protected routes.

### `POST /activity`

```json
// Request
{
  "type": "running",
  "durationMinutes": 45,
  "caloriesBurned": 380.5,
  "notes": "Morning run in the park"
}

// Response 201
{
  "status": 201,
  "message": "Activity logged successfully",
  "data": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### `POST /health`

```json
// Request
{
  "weightKg": 75.5,
  "heightCm": 178.0,
  "heartRate": 68,
  "systolic": 120,
  "diastolic": 80,
  "notes": "Morning check-in"
}

// Response 201 — BMI is auto-computed from weightKg and heightCm
{
  "status": 201,
  "message": "Health metric recorded successfully",
  "data": "f7e8d9c0-a1b2-3456-cdef-789012345678"
}
```

### Error Response

```json
// 404 Not Found
{
  "status": 404,
  "message": "Activity not found with id: abc-123",
  "data": null
}

// 403 Forbidden
{
  "status": 403,
  "message": "Access denied: you do not own this resource",
  "data": null
}
```

---

## 🗄️ Firestore Data Structure

```
Firestore
├── users/
│   └── {userId}/
│       ├── id          (string)   UUID
│       ├── username    (string)
│       ├── email       (string)
│       ├── password    (string)   BCrypt hash — never plain text
│       ├── role        (string)   "USER"
│       ├── active      (boolean)
│       └── createdAt   (string)   ISO-8601
│
├── activities/
│   └── {activityId}/
│       ├── id              (string)
│       ├── userId          (string)   foreign key → users/{userId}
│       ├── type            (string)   running | cycling | swimming | ...
│       ├── durationMinutes (integer)
│       ├── caloriesBurned  (double)
│       ├── notes           (string)   optional
│       └── loggedAt        (string)   ISO-8601
│
└── health_metrics/
    └── {metricId}/
        ├── id          (string)
        ├── userId      (string)   foreign key → users/{userId}
        ├── weightKg    (double)
        ├── heightCm    (double)
        ├── bmi         (double)   auto-computed: weight / (height in m)²
        ├── heartRate   (integer)  bpm
        ├── systolic    (integer)  mmHg
        ├── diastolic   (integer)  mmHg
        ├── notes       (string)   optional
        └── recordedAt  (string)   ISO-8601
```

---

## 🔒 Security

- Passwords are hashed with **BCrypt** before storage — plain text is never persisted
- JWT tokens are signed with **HMAC-SHA256** and expire after **24 hours**
- Every protected route validates the token via `JwtAuthFilter` (extends `OncePerRequestFilter`) before reaching the controller
- **Ownership is enforced at the service layer** — users can only read or delete their own resources
- `serviceAccountKey.json` is excluded from Git via `.gitignore`
- DNS configured to `8.8.8.8 / 8.8.4.4 / 1.1.1.1` in Docker containers to ensure stable connectivity to Google APIs
- **CORS** restricts allowed origins in production to the registered frontend URL

---

## 🚀 Deployment

### Backend — Railway

```bash
# 1. Push code to GitHub (without serviceAccountKey.json)
# 2. Create a new Railway project and link your GitHub repository
# 3. Add environment variables in the Railway dashboard:

JWT_KEY=your-secret-key-256-bits-minimum
FIREBASE_CREDENTIALS={"type":"service_account","project_id":"..."}  # full JSON
GROQ_API_KEY=your-groq-api-key

# 4. Railway auto-detects Maven and deploys automatically on every push
```

### Frontend — Vercel

```bash
# 1. Import your GitHub repo on vercel.com
# 2. Set the root directory to: frontend/
# 3. Add the environment variable:

VITE_API_URL=https://your-railway-app.up.railway.app/api/vitatrack/v1

# 4. Vercel deploys automatically on every push to main
```

### Docker Hub — Publish your own images

```bash
# Build and tag backend
docker build -t achoyatte2025/vitatrack-backend:latest ./backend

# Build and tag frontend
docker build \
  --build-arg VITE_API_URL=https://your-api-url/api/vitatrack/v1 \
  -t achoyatte2025/vitatrack-frontend:latest \
  ./frontend

# Push to Docker Hub
docker login
docker push achoyatte2025/vitatrack-backend:latest
docker push achoyatte2025/vitatrack-frontend:latest
```

---

## 👤 Author

**ACHO YATTE**

- 🐙 GitHub: [@ACHOYATTE2025](https://github.com/ACHOYATTE2025)
- 🐳 Docker Hub: [achoyatte2025](https://hub.docker.com/u/achoyatte2025)
- 📦 Repository: [ACHOYATTE2025/Vitatrack](https://github.com/ACHOYATTE2025/Vitatrack)

---

> Built with ☕ Java, ⚛️ React, 🔥 Firebase, and 🐳 Docker.
