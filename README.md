Here is the perfected, winning `README.md`. 


```md
# DispatchHub — Ride Dispatch & Driver Matching System
### 🏆 A CredX Hiring Hackathon 2.0 Submission by Team UnityNimit

A real-time ride-hailing dispatch core inspired by Uber's trip lifecycle. Riders request trips, the system matches available drivers, trip state moves through a defined lifecycle, fares are estimated server-side, and admins get an operations dashboard to monitor live trips and drivers.

---

## 🚀 Quick Start (Docker)

We have fully Dockerized the application for an instant, 1-click boot. You do not need to manually install PostgreSQL, Node, or Maven.

Ensure Docker and Docker Compose are installed, then simply run:
```bash
docker-compose up --build
```
* **Frontend App:** `http://localhost:4200`
* **Backend API:** `http://localhost:8080/api`

*(Logins provided at the bottom of this README).*

---

## Table of contents

- [Project Overview](#project-overview)
- [Hackathon Achievements & Optimizations](#hackathon-achievements--optimizations)
- [Architecture](#architecture)
- [API Flow](#api-flow)
- [Manual Setup Instructions](#manual-setup-instructions)

---

## Project Overview

DispatchHub models three user roles:

- **RIDER** — requests trips, sees a live reactive fare estimate, tracks trip status in real-time, rates drivers.
- **DRIVER** — goes online/offline, accepts nearby trip requests, moves a trip through its lifecycle.
- **ADMIN** — monitors live trips and drivers from an ops dashboard, views analytics, and force-cancels stuck trips.

A trip moves through: `REQUESTED → ACCEPTED → ARRIVED → IN_PROGRESS → COMPLETED`,
with `CANCELLED` reachable from any non-terminal state.

---

## 🌟 Hackathon Achievements & Optimizations

We identified the intentional traps left in the baseline code and completely re-architected the bottlenecks to ensure the system scales to millions of users. 

### 1. Database & Algorithmic Optimization
* **Fixed the N+1 Analytics Bomb:** The original `AnalyticsService` loaded *every single trip* into Java memory to group driver stats. We replaced this with a highly optimized JPQL `SELECT NEW` aggregation query, pushing the calculation entirely down to PostgreSQL and reducing memory usage to `O(1)`.
* **Fixed N+1 Associations:** Replaced default Hibernate `findAll()` methods with `@EntityGraph` annotations to fetch Riders, Drivers, and Users in a single, clean SQL `JOIN`.
* **Haversine Bounding-Box Filtering:** For the "Nearby Drivers" and "Nearby Trips" logic, we implemented a server-side Geographic Bounding Box pre-filter combined with native SQL Haversine math, ensuring we only pull highly relevant rows from the DB.

### 2. Concurrency & Security
* **Pessimistic Locking:** The baseline had a race condition where two drivers could accept the same trip. We implemented `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT FOR UPDATE`) on the Trip row during acceptance to guarantee atomic consistency without throwing unnecessary frontend errors.
* **Strict Ownership Validation:** Locked down the API so Riders can only cancel/review *their own* trips, and Drivers can only interact with trips assigned to *them*.
* **Sliding-Window Rate Limiting:** Built an in-memory `TripRequestRateLimiter` protecting the `POST /api/trips` endpoint from abuse (Max 5 requests per minute per rider), returning `429 Too Many Requests`.

### 3. "Wow Factor" Enhancements
* **Real-Time WebSockets (STOMP):** Ripped out the slow 5-second HTTP interval polling on the frontend. We implemented a persistent WebSocket connection using `spring-boot-starter-websocket` and `@stomp/rx-stomp`. When a driver accepts a ride, the rider's screen updates in milliseconds.
* **Reactive Fare Estimation:** Converted the Rider's booking form to use RxJS `valueChanges` with `debounceTime` and `switchMap`. The fare estimate now updates in real-time as the user types without spamming the backend.
* **Full Feature Completion:** Finished all missing `TODO`s including Admin Force Cancel, Driver Reviews (with atomic rating recalculation), Incoming Request views, and Rider Trip History.

---

## Architecture

### Backend
- **Java 21**, **Spring Boot 3.3.x**, Maven.
- **Spring Security** with stateless **JWT** auth (`Authorization: Bearer <token>`).
- **WebSockets** via STOMP for live trip tracking.
- **Spring Data JPA** + **PostgreSQL**, layered `Controller → Service → Repository → Entity`.
- DTOs at every controller boundary — entities are never serialized directly.
- `@RestControllerAdvice` global exception handler with a consistent error shape.

### Frontend
- **Angular 20+**, fully **standalone components** (no `NgModule`s).
- **Angular Material** for UI (`mat-sidenav`, `mat-toolbar`, `mat-table`, etc.).
- **RxJS** + `HttpClient` in a dedicated services layer (`core/services`).
- **Angular Signals** for current-user auth state and reactive UI updates.
- **SockJS / RxStomp** for real-time WebSocket subscriptions.

### Database
PostgreSQL. Core tables: `users`, `driver_profiles`, `rider_profiles`, `trips`, `trip_status_history`, `reviews`. 

---

## API Flow

1. Client calls `POST /api/auth/login` (or `/register`) → receives a JWT.
2. Every subsequent HTTP request carries `Authorization: Bearer <token>`.
3. WebSocket connections pass the token via connection headers.
4. `JwtAuthenticationFilter` validates the token and populates the Spring Security context with a `UserPrincipal`.

---

## Manual Setup Instructions

*If you prefer not to use Docker, follow these steps to run locally.*

### Prerequisites
- Java 21 (JDK)
- Maven 3.9+ 
- Node.js 20+ and npm 10+
- PostgreSQL 15+ running locally

### Database
1. Create the database and a role:
   ```sql
   CREATE DATABASE dispatchhub;
   CREATE USER dispatchhub WITH PASSWORD 'dispatchhub';
   GRANT ALL PRIVILEGES ON DATABASE dispatchhub TO dispatchhub;
   ```

### Backend
```bash
cd backend
cp application-example.env .env
mvn spring-boot:run
```
*(The API starts on `http://localhost:8080`. The database will be seeded automatically on the first boot).*

### Frontend
```bash
cd frontend
npm install
npm start
```
*(The app starts on `http://localhost:4200`)*

### Test Accounts
You can register a new account, or use the automatically seeded accounts:
* **Admin:** `admin@dispatchhub.com` / `Admin123!`
* **Rider:** `rider1@dispatchhub.com` / `Rider123!`
* **Driver:** `driver1@dispatchhub.com` / `Driver123!`
```
