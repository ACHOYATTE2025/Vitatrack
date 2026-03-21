# 🏃 VitaTrack — Health & Fitness Tracker

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat-square&logo=spring-boot)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk)
![Firestore](https://img.shields.io/badge/Firestore-Cloud-FFCA28?style=flat-square&logo=firebase)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

> A cloud-native REST API for tracking physical activity and health metrics — built with Spring Boot 3, secured with JWT, and powered by Google Cloud Firestore.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Request & Response Examples](#request--response-examples)
- [Environment Variables](#environment-variables)
- [Local Setup](#local-setup)
- [Firestore Data Structure](#firestore-data-structure)
- [Security](#security)
- [Deployment](#deployment)
- [Author](#author)

---

## Overview

VitaTrack is a health and fitness tracking application that stores all user data in the cloud. Users can register, log physical activities (running, cycling, swimming...), record health metrics (weight, BMI, heart rate, blood pressure), and retrieve their personal history securely.

**Key design decisions:**
- No relational database — all data lives in **Firestore** collections
- Stateless authentication via **JWT** — no sessions, no cookies
- Ownership verification on every request — users can only access their own data
- BMI is **auto-computed** from weight and height on every health metric entry

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (jjwt 0.12.3) |
| Database | Google Cloud Firestore (Firebase Admin SDK 9.2.0) |
| Password hashing | BCrypt |
| Build tool | Maven |
| Frontend | React + Vite |
| Deployment | Railway (backend) · Vercel (frontend) |

---

## Project Structure
```
vitatrack/
├── backend/
│   └── src/main/java/com/portfolio/VitaTrack/
│       ├── Config/
│       │   ├── FirebaseConfig.java        # Firebase Admin SDK initialization
│       │   └── SecurityConfig.java        # Spring Security + CORS configuration
│       ├── Controllers/
│       │   ├── AuthController.java        # POST /register, POST /login
│       │   ├── ActivityController.java    # CRUD /activity
│       │   └── HealthMetricController.java# CRUD /health
│       ├── Services/
│       │   ├── FirestoreService.java      # Generic Firestore CRUD wrapper
│       │   ├── JwtService.java            # Token generation + validation
│       │   ├── AuthService.java           # Register + login logic
│       │   ├── ActivityService.java       # Activity business logic
│       │   └── HealthMetricService.java   # Health metric business logic
│       ├── Security/
│       │   └── JwtAuthFilter.java         # JWT filter (OncePerRequestFilter)
│       ├── Dto/
│       │   ├── SignupRequestDto.java
│       │   ├── LoginRequestDto.java
│       │   ├── ActivityRequestDto.java
│       │   ├── HealthMetricRequestDto.java
│       │   └── ResponseDto.java
│       └── Exception/
│           ├── ApplicationAdvice.java     # Global exception handler (@RestControllerAdvice)
│           └── ResourceNotFoundException.java
└── frontend/                              # React + Vite client
```

---

## API Reference

**Base URL:** `/api/vitatrack/v1`

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/auth/register` | Public | Create a new user account |
| `POST` | `/auth/login` | Public | Authenticate and receive a JWT token |

### Activity Tracking

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/activity` | 🔒 JWT | Log a new physical activity |
| `GET` | `/activity` | 🔒 JWT | Get all activities for the current user |
| `GET` | `/activity/{id}` | 🔒 JWT | Get a single activity by ID |
| `DELETE` | `/activity/{id}` | 🔒 JWT | Delete an activity |

### Health Metrics

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/health` | 🔒 JWT | Record a new health metric |
| `GET` | `/health` | 🔒 JWT | Get all health metrics for the current user |
| `GET` | `/health/{id}` | 🔒 JWT | Get a single metric by ID |
| `DELETE` | `/health/{id}` | 🔒 JWT | Delete a health metric |

---

## Request & Response Examples

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

> Use the token from `data` as a Bearer token on all protected routes:
> `Authorization: Bearer <token>`

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
  "data": "a1b2c3d4-e5f6-..."
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
  "data": "f7e8d9c0-..."
}
```

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `JWT_KEY` | ✅ Always | Secret key for signing JWT tokens (min 256 bits) |
| `FIREBASE_CREDENTIALS` | ✅ Production | Full JSON content of `serviceAccountKey.json` |

**Local development:** place `serviceAccountKey.json` in `src/main/resources/` — the app loads it automatically.

**Production (Railway):** paste the entire JSON content of `serviceAccountKey.json` as the value of `FIREBASE_CREDENTIALS`.

> ⚠️ Never commit `serviceAccountKey.json` to Git. It is already listed in `.gitignore`.

---

## Local Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+ (frontend only)
- A Firebase project with **Firestore enabled in test mode**
- `serviceAccountKey.json` downloaded from Firebase Console → Project Settings → Service Accounts

### Backend
```bash
# Clone the repository
git clone https://github.com/ACHOYATTE2025/vitatrack.git
cd vitatrack/backend

# Place serviceAccountKey.json in src/main/resources/

# Set the JWT secret (Linux/macOS)
export JWT_KEY="vitatrack-secret-key-256-bits-minimum-length-required"

# Set the JWT secret (Windows PowerShell)
$env:JWT_KEY="vitatrack-secret-key-256-bits-minimum-length-required"

# Run
mvn spring-boot:run

# API available at:
# http://localhost:8080/api/vitatrack/v1
```

### Frontend
```bash
cd vitatrack/frontend
npm install
npm run dev

# App available at: http://localhost:5173
```

---

## Firestore Data Structure
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
│       ├── userId          (string)   links to users/{userId}
│       ├── type            (string)   running, cycling, swimming...
│       ├── durationMinutes (integer)
│       ├── caloriesBurned  (double)
│       ├── notes           (string)   optional
│       └── loggedAt        (string)   ISO-8601
│
└── health_metrics/
    └── {metricId}/
        ├── id          (string)
        ├── userId      (string)   links to users/{userId}
        ├── weightKg    (double)
        ├── heightCm    (double)
        ├── bmi         (double)   auto-computed
        ├── heartRate   (integer)  bpm
        ├── systolic    (integer)  mmHg
        ├── diastolic   (integer)  mmHg
        ├── notes       (string)   optional
        └── recordedAt  (string)   ISO-8601
```

---

## Security

- Passwords are hashed with **BCrypt** before storage — plain text is never persisted
- JWT tokens are signed with **HMAC-SHA256** and expire after **24 hours**
- Every protected route validates the token via `JwtAuthFilter` before reaching the controller
- **Ownership is enforced** — users can only read or delete their own activities and metrics
- `serviceAccountKey.json` is excluded from Git via `.gitignore`
- CORS restricts allowed origins in production to the registered frontend URL

---

## Deployment

### Railway (backend)
```bash
# 1. Push your code to GitHub (without serviceAccountKey.json)
# 2. Create a new Railway project and connect your repository
# 3. Add environment variables in Railway dashboard:

JWT_KEY=your-secret-key-256-bits-minimum
FIREBASE_CREDENTIALS={"type":"service_account","project_id":"..."}  # full JSON

# 4. Railway auto-detects Maven and deploys automatically
```

### Vercel (frontend)
```bash
# 1. Connect your GitHub repo to Vercel
# 2. Set the root directory to frontend/
# 3. Add environment variable:

VITE_API_URL=https://your-railway-app.up.railway.app/api/vitatrack/v1
```

---

## Author

**ACHO YATTE**
- GitHub: [@ACHOYATTE2025](https://github.com/ACHOYATTE2025)
- Project: VitaTrack — Health & Fitness Tracker
- Stack: Java · Spring Boot · React · Firebase · JWT

---

