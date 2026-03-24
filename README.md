# 🏃 VitaTrack — Health & Fitness Tracker

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=flat-square&logo=vite)](https://vitejs.dev/)
[![Firestore](https://img.shields.io/badge/Firestore-Cloud-FFCA28?style=flat-square&logo=firebase)](https://firebase.google.com/docs/firestore)
[![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens)](https://jwt.io/)
[![Groq AI](https://img.shields.io/badge/AI-Groq-F55036?style=flat-square&logo=lightning)](https://groq.com/)
[![Docker](https://img.shields.io/badge/Docker-Hub-2496ED?style=flat-square&logo=docker)](https://hub.docker.com/u/achoyatte2025)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

> **VitaTrack** is a cloud-native full-stack application for tracking physical activities and health metrics — enriched with **AI-powered recommendations via the Groq API**. Built with Spring Boot 3, secured with JWT, powered by Google Cloud Firestore, and distributed via **Docker Hub**.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Live Demo & Links](#-live-demo--links)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [AI Recommendations — Groq API](#-ai-recommendations--groq-api)
- [Docker Hub — Deploy in 2 minutes](#-docker-hub--deploy-in-2-minutes)
- [Local Setup (from source)](#-local-setup-from-source)
- [Environment Variables](#-environment-variables)
- [API Reference](#-api-reference)
- [Request & Response Examples](#-request--response-examples)
- [Firestore Data Structure](#-firestore-data-structure)
- [Security](#-security)
- [Author](#-author)

---

## 🌟 Overview

VitaTrack lets users register, log physical activities (running, cycling, swimming, etc.), record health metrics (weight, BMI, heart rate, blood pressure), and receive **personalized AI-generated fitness recommendations** — all secured and stored in the cloud.

**Key design decisions:**

- No relational database — all data lives in **Firestore** collections (serverless, scalable, zero maintenance)
- Stateless authentication via **JWT** — no sessions, no cookies
- **Ownership enforcement** on every request — users can only access their own data
- **BMI is auto-computed** from weight and height on every health metric entry
- **Groq API** powers instant AI-generated fitness and health recommendations
- **Fully Dockerized** — the entire stack runs with a single command via Docker Hub images

---

## 🎬 Live Demo & Links

| Resource | Link |
|---|---|
| 🐙 GitHub Repository | [github.com/ACHOYATTE2025/Vitatrack](https://github.com/ACHOYATTE2025/Vitatrack) |
| 🐳 Docker Hub — Backend | [hub.docker.com/r/achoyatte2025/vitatrack-backend](https://hub.docker.com/r/achoyatte2025/vitatrack-backend) |
| 🐳 Docker Hub — Frontend | [hub.docker.com/r/achoyatte2025/vitatrack-frontend](https://hub.docker.com/r/achoyatte2025/vitatrack-frontend) |
| 🎥 Demo Video | ▶️ *[Add your YouTube or Loom link here]* |

### 🎥 Video Demonstration

> *(Replace `YOUR_VIDEO_ID` with your actual YouTube video ID once uploaded)*

[![VitaTrack Demo](https://www.youtube.com/watch?v=6WcUkebgXEM)]

> 🎙️ The demo covers: user registration, activity logging, health metric tracking, and AI-powered recommendations from the Groq API.

---

### 📸 Application Screenshots

> *(Drag & drop your screenshots directly into GitHub's editor to add them)*

| Login | Dashboard | Activity Tracker |
|:---:|:---:|:---:|
| ![Login](docs/screenshots/login.png) | ![Dashboard](docs/screenshots/dashboard.png) | ![Activity](docs/screenshots/activity.png) |

| Health Metrics | AI Recommendations | |
|:---:|:---:|:---:|
| ![Health](docs/screenshots/health.png) | ![AI Reco](docs/screenshots/recommendations.png) | |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Client Browser                   │
└────────────────────┬────────────────────────────────┘
                     │ HTTP / REST
┌────────────────────▼────────────────────────────────┐
│         Frontend — React + Vite (nginx)             │
│         Docker: vitatrack-frontend:latest           │
│      Port 80  ·  Proxies /api/* → backend:8080      │
└────────────────────┬────────────────────────────────┘
                     │ Internal Docker network
┌────────────────────▼────────────────────────────────┐
│           Backend — Spring Boot 3                   │
│          Docker: vitatrack-backend:latest           │
│  Spring Security · JWT Filter · REST Controllers   │
│            GroqService (AI recommendations)         │
└──────────┬─────────────────────────┬────────────────┘
           │ Firebase Admin SDK      │ HTTPS REST
┌──────────▼──────────┐   ┌─────────▼──────────────┐
│  Google Firestore   │   │       Groq API          │
│  users/             │   │  llama-3.3-70b-versatile│
│  activities/        │   │  instant AI inference   │
│  health_metrics/    │   └────────────────────────┘
└─────────────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.2.5 |
| Security | Spring Security + JWT (jjwt) | 0.12.3 |
| Database | Google Cloud Firestore (Firebase Admin SDK) | 9.2.0 |
| AI Engine | Groq API (`llama-3.3-70b-versatile`) | — |
| Password hashing | BCrypt | — |
| Build tool | Maven | 3.8+ |
| Frontend | React + Vite | 18 / 5 |
| Web server | Nginx | Alpine |
| Containerization | Docker + Docker Compose | — |
| Registry | Docker Hub | — |

---

## 📁 Project Structure

```
vitatrack/
├── backend/
│   ├── Dockerfile                           # Multi-stage build (Maven → JRE 17 slim)
│   └── src/main/java/com/portfolio/VitaTrack/
│       ├── Config/
│       │   ├── FirebaseConfig.java          # Firebase Admin SDK initialization
│       │   └── SecurityConfig.java          # Spring Security + CORS configuration
│       ├── Controllers/
│       │   ├── AuthController.java          # POST /register, POST /login
│       │   ├── ActivityController.java      # CRUD /activity
│       │   ├── HealthMetricController.java  # CRUD /health
│       │   └── RecommendationController.java# GET /recommendations (Groq AI)
│       ├── Services/
│       │   ├── FirestoreService.java        # Generic Firestore CRUD wrapper
│       │   ├── JwtService.java              # Token generation + validation
│       │   ├── AuthService.java             # Register + login logic
│       │   ├── ActivityService.java         # Activity business logic
│       │   ├── HealthMetricService.java     # Health metric business logic
<<<<<<< HEAD
│       │   └── GroqService.java             # Groq API client (AI recommendations)
=======
│       │   └── RecommandationService.java             # Groq API client (AI recommendations)
>>>>>>> d560dcfd3a87872e45779966224abe95302ca7c2
│       ├── Security/
│       │   └── JwtAuthFilter.java           # JWT filter (OncePerRequestFilter)
│       ├── Dto/
│       │   ├── SignupRequestDto.java
│       │   ├── LoginRequestDto.java
│       │   ├── ActivityRequestDto.java
│       │   ├── HealthMetricRequestDto.java
│       │   └── ResponseDto.java
│       └── Exception/
│           ├── ApplicationAdvice.java       # Global exception handler
│           └── ResourceNotFoundException.java
│
├── frontend/
│   ├── Dockerfile                           # Multi-stage build (Node → nginx Alpine)
│   └── src/                                # React + Vite source
│
├── docker-compose.yml                       # Local dev stack (builds from source)
├── docker-compose.prod.yml                  # Production stack (Docker Hub images)
├── env.example                              # Environment variable template
└── README.md
```

---

## 🤖 AI Recommendations — Groq API

VitaTrack integrates **[Groq](https://groq.com/)** to deliver instant, personalized fitness and health recommendations based on the user's real data.

### How it works

1. User calls `GET /recommendations` with their JWT token
2. Backend fetches their latest **activities** and **health metrics** from Firestore
3. A structured prompt is built and sent to the **Groq API** (`llama-3.3-70b-versatile`)
4. Groq returns a personalized recommendation in under a second (LPU-based inference)
5. The result is returned directly to the frontend

### Why Groq?

| Feature | Benefit |
|---|---|
| LPU-based inference | Sub-second response times |
| Free tier available | No upfront cost for development |
| `llama-3.3-70b-versatile` | High-quality health & fitness reasoning |
| Simple REST API | Easy Spring Boot integration |

### Example recommendation outputd

### `POST /activity`

```json
// Request
{ "type": "running", "durationMinutes": 45, "caloriesBurned": 380.5, "notes": "Morning run" }

// Response 201
{ "status": 201, "message": "Activity logged successfully", "data": "a1b2c3d4-..." }
```

### `POST /health`

```json
// Request
{ "weightKg": 75.5, "heightCm": 178.0, "heartRate": 68, "systolic": 120, "diastolic": 80 }

// Response 201 — BMI auto-computed from weightKg and heightCm
{ "status": 201, "message": "Health metric recorded successfully", "data": "f7e8d9c0-..." }
```

### `GET /recommendations`

```json
// Response 200
{
  "status": 200,
  "message": "AI recommendations generated successfully",
  "data": "Based on your data (3 running sessions, BMI 24.1, HR 68 bpm):\n1. Add a rest day between runs.\n2. Increase session duration by 5 min/week.\n3. Aim for 2.5L of water daily."
}
```

### Error Responses

```json
{ "status": 404, "message": "Activity not found with id: abc-123", "data": null }
{ "status": 403, "message": "Access denied: you do not own this resource", "data": null }
{ "status": 401, "message": "JWT token is expired or invalid", "data": null }
```

---

## 🗄️ Firestore Data Structure

```
Firestore
├── users/
│   └── {userId}/
│       ├── id, username, email
│       ├── password      BCrypt hash — never plain text
│       ├── role          "USER"
│       ├── active        boolean
│       └── createdAt     ISO-8601
│
├── activities/
│   └── {activityId}/
│       ├── id, userId    → users/{userId}
│       ├── type          running | cycling | swimming | ...
│       ├── durationMinutes, caloriesBurned
│       ├── notes         optional
│       └── loggedAt      ISO-8601
│
└── health_metrics/
    └── {metricId}/
        ├── id, userId    → users/{userId}
        ├── weightKg, heightCm
        ├── bmi           auto-computed: weight / (height in m)²
        ├── heartRate     bpm
        ├── systolic, diastolic   mmHg
        ├── notes         optional
        └── recordedAt    ISO-8601
```

---

## 🔒 Security

- Passwords hashed with **BCrypt** — plain text never stored
- JWT signed with **HMAC-SHA256**, expires after **24 hours**
- `JwtAuthFilter` (`OncePerRequestFilter`) validates every protected request before it reaches the controller
- **Ownership enforced at the service layer** — users can only access their own data
- `serviceAccountKey.json` excluded from Git via `.gitignore`
- Docker DNS set to `8.8.8.8 / 8.8.4.4 / 1.1.1.1` for reliable access to Firestore and Groq APIs from containers
- CORS restricts allowed origins in production

---

## 👤 Author

**ACHO YATTE**

- 🐙 GitHub: [@ACHOYATTE2025](https://github.com/ACHOYATTE2025)
- 🐳 Docker Hub: [hub.docker.com/u/achoyatte2025](https://hub.docker.com/u/achoyatte2025)
- 📦 Repository: [ACHOYATTE2025/Vitatrack](https://github.com/ACHOYATTE2025/Vitatrack)

---

> Built with ☕ Java · ⚛️ React · 🔥 Firebase · ⚡ Groq AI · 🐳 Docker
