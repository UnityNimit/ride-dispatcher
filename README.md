# DispatchHub — Ride Dispatch & Driver Matching System

A ride-hailing dispatch core inspired by Uber's trip lifecycle: riders
request trips, the system matches available drivers, trip state moves
through a defined lifecycle, fares are estimated server-side, and admins get
an operations dashboard to monitor live trips and drivers.

This is a **Round-2 hackathon challenge codebase**. Roughly 70% of the app is
built and working end-to-end; the rest is intentionally incomplete or buggy
for you to find, fix, and extend within the time box. Read this whole file
before you start — it tells you what's already working, what's known to be
broken, and what's missing outright.

---

## Table of contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Existing Features](#existing-features)
- [Known Bugs](#known-bugs)
- [Missing Features](#missing-features)
- [Enhancement Opportunities](#enhancement-opportunities)
- [Evaluation Criteria](#evaluation-criteria)
- [Setup Instructions](#setup-instructions)

---

## Project Overview

DispatchHub models three user roles:

- **RIDER** — requests trips, sees a live fare estimate, tracks trip status, rates drivers.
- **DRIVER** — goes online/offline, accepts trip requests, moves a trip through its lifecycle.
- **ADMIN** — monitors live trips and drivers from an ops dashboard, views analytics.

A trip moves through: `REQUESTED → ACCEPTED → ARRIVED → IN_PROGRESS → COMPLETED`,
with `CANCELLED` reachable from any non-terminal state.

---

## Architecture

### Backend

- **Java 21**, **Spring Boot 3.3.x**, Maven.
- **Spring Security** with stateless **JWT** auth (`Authorization: Bearer <token>`).
- **Spring Data JPA** + **PostgreSQL**, layered `Controller → Service → Repository → Entity`.
- DTOs at every controller boundary — entities are never serialized directly.
- Bean Validation (`@NotNull`, `@Size`, `@Pattern`, etc.) on request DTOs.
- `@RestControllerAdvice` global exception handler with a consistent error shape.
- Pagination via Spring Data `Pageable` on trip and driver list endpoints.

### Frontend

- **Angular 20+**, fully **standalone components** (no `NgModule`s).
- **Angular Material** for UI (`mat-sidenav`, `mat-toolbar`, `mat-table`, etc.).
- **RxJS** + `HttpClient` in a dedicated services layer (`core/services`).
- **Angular Signals** for current-user auth state and for the polled live
  trip status on the trip detail page.
- Functional route guards (`CanActivateFn`) for auth and role-based routing.
- An `HttpInterceptorFn` attaches the JWT to outgoing API requests.

### Database

PostgreSQL. See [`docs/DATABASE-SCHEMA.sql`](docs/DATABASE-SCHEMA.sql) for
`CREATE TABLE` statements and [`docs/ER-DIAGRAM.md`](docs/ER-DIAGRAM.md) for
the entity-relationship diagram. Core tables: `users`, `driver_profiles`,
`rider_profiles`, `trips`, `trip_status_history`, `reviews`.

### API Flow

1. Client calls `POST /api/auth/login` (or `/register`) → receives a JWT.
2. Every subsequent request carries `Authorization: Bearer <token>`.
3. `JwtAuthenticationFilter` validates the token and populates the Spring
   Security context with a `UserPrincipal` (id, email, role).
4. Controllers use `@PreAuthorize` (method-level) plus the `SecurityConfig`
   filter chain (URL-level) for role checks, and pull the current user via
   the `CurrentUser` helper bean.
5. Full endpoint reference: [`docs/API-DOCUMENTATION.md`](docs/API-DOCUMENTATION.md).

### Folder Structure

Full annotated tree: [`docs/FOLDER-STRUCTURE.md`](docs/FOLDER-STRUCTURE.md).

```
ride-dispatch/
├── backend/            Spring Boot API (Maven)
├── frontend/            Angular 20 SPA
├── docs/                 ER diagram, API docs, schema, folder structure
├── postman/              Postman collection
├── seed-data/            Raw SQL seed data
├── README.md              You are here
└── JUDGE-SOLUTION-GUIDE.md   Judges only - not meant for candidates
```

---

## Existing Features

**Backend**
- JWT auth: register + login, working end to end, token expiry enforced.
- Role-based access control via Spring Security (`RIDER` / `DRIVER` / `ADMIN`).
- Full CRUD-equivalent flows for Trip, DriverProfile, RiderProfile.
- Trip lifecycle endpoints: request, accept, arrive, start, complete, cancel.
- Fare estimation endpoint (base fare + per-km rate + per-minute rate).
- Driver availability toggle (go online/offline) and location update.
- Paginated trip list and driver list endpoints.
- Admin dashboard stats endpoint and a trips-per-driver analytics endpoint.
- Global exception handling with a consistent JSON error shape.
- `DataLoader` (`CommandLineRunner`) seeds realistic dev data on first boot.

**Frontend**
- Login / register pages wired to the real backend, JWT stored client-side.
- Role-based functional route guards (`authGuard`, `guestGuard`, `roleGuard`).
- Main layout: `mat-sidenav` + `mat-toolbar`, nav links driven by role.
- Admin dashboard with live stat cards pulled from the backend.
- Trips list (admin): paginated Material table with a status filter.
- Trip detail page: trip info + status timeline, polled on an interval and
  held in an Angular Signal.
- Driver management page (admin): paginated Material table of drivers.
- "Request a ride" form with a fare estimate preview panel.
- `AuthService` / `TripService` / `DriverService` / `RiderService` /
  `DashboardService`, all typed against models matching the backend DTOs.



## Missing Features

These are not implemented. Where a stub exists, it's marked with a `TODO`
comment and typically throws `UnsupportedOperationException` (mapped to
`501 Not Implemented`) or is a placeholder UI shell.

- **Backend**: "Nearby available drivers" query given a rider's lat/lng —
  `DriverService.findNearbyAvailableDrivers` is a stub; `GET /api/drivers/nearby`
  currently 501s.
- **Backend**: Driver rating/review submission after trip completion — the
  `Review` entity and repository exist, `ReviewService.submitReview` is a
  stub, `POST /api/trips/{id}/review` currently 501s.
- **Backend**: Admin "force-cancel / reassign a stuck trip" recovery
  endpoint — not implemented at all (`POST /api/admin/trips/{id}/force-cancel`
  501s).
- **Frontend**: Driver-facing "incoming trip request" page — routed and
  present as a placeholder shell (`IncomingRequestComponent`), no actual
  polling/data wired up.
- **Frontend**: Rider trip history page — routed and present as a
  placeholder shell (`TripHistoryComponent`); `TripService.getMyTrips` and
  the backing `GET /api/trips/my` endpoint already work, this page just
  doesn't call it yet.
- **Frontend**: The fare estimate reactivity bug under Known Bugs (#6) is
  also a feature-completion task — the preview should genuinely update as
  the rider edits the form.

---

## Enhancement Opportunities

Not required, but fair game if you have time left and want to stand out:

- Rate limiting on `POST /api/trips` to prevent request spam (not implemented anywhere).
- Real-time trip updates via WebSocket/SSE instead of interval polling
  (there's a TODO left in `TripDetailComponent` marking where this would plug in).
- Geocoding integration so "Request a ride" takes real addresses instead of
  fixed placeholder coordinates.
- Driver earnings/payout view.
- Surge pricing based on real-time supply/demand instead of a static multiplier.
- Refresh tokens instead of a single long-lived JWT.
- Integration/contract tests for the trip state machine.
- Dockerizing backend + frontend + Postgres for one-command local startup.

---

## Evaluation Criteria

You'll be assessed on:

- **Correctness** — do your fixes actually fix the underlying bug, not just
  hide the symptom?
- **Code quality** — does your code fit the existing architecture and
  conventions, or does it look bolted on?
- **Completeness** — how many of the missing features / bugs did you get to
  in the time available, and how well?
- **UI/UX judgment** — for frontend work, does it look and feel consistent
  with the rest of the app?
- **Testing/verification** — did you check your fix actually works (manual
  testing is fine; automated tests are a bonus)?
- **Communication** — clear commit messages / PR description of what you
  changed and why.

You are not expected to finish everything. Prioritize.

---

## Setup Instructions

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+ (or use your IDE's bundled Maven)
- Node.js 20+ and npm 10+
- PostgreSQL 15+ running locally (or via Docker)
- Angular CLI 20+ (`npm install -g @angular/cli`) — optional, `npx ng` also works

### Database

1. Create the database and a role:
   ```sql
   CREATE DATABASE dispatchhub;
   CREATE USER dispatchhub WITH PASSWORD 'dispatchhub';
   GRANT ALL PRIVILEGES ON DATABASE dispatchhub TO dispatchhub;
   ```
2. Apply the schema:
   ```bash
   psql -U dispatchhub -d dispatchhub -f docs/DATABASE-SCHEMA.sql
   ```
3. (Optional) Load seed data directly via SQL instead of relying on the
   backend's `DataLoader`:
   ```bash
   psql -U dispatchhub -d dispatchhub -f seed-data/seed.sql
   ```
   Note: if you run the backend against an **empty** database, `DataLoader`
   seeds its own dev dataset automatically on first boot — you don't need to
   run both. Use `seed-data/seed.sql` if you want the exact dataset described
   in this repo's docs (it includes an ACCEPTED trip and a second historical
   COMPLETED trip that `DataLoader` doesn't create), or if you're loading
   data outside of the Spring Boot app entirely.

### Environment Variables

Copy the example env file and adjust as needed:

```bash
cp backend/application-example.env backend/.env
```

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/dispatchhub` |
| `DB_USERNAME` | DB user | `dispatchhub` |
| `DB_PASSWORD` | DB password | `dispatchhub` |
| `JWT_SECRET` | Base64-encoded HMAC-SHA256 key | (dev-only value in the example file) |
| `JWT_EXPIRATION_MS` | Token lifetime in ms | `86400000` (24h) |
| `JWT_ISSUER` | `iss` claim value | `dispatchhub-api` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:4200` |

`application.yml` reads all of these via `${VAR:default}` placeholders, so
the app runs with sane defaults even if you don't set anything explicitly
for local dev.

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. Health check:
`GET http://localhost:8080/actuator/health`.

To build a jar:
```bash
mvn clean package
java -jar target/dispatchhub-0.1.0.jar
```

### Frontend

```bash
cd frontend
npm install
npm start
```

The app starts on `http://localhost:4200` and expects the backend at
`http://localhost:8080/api` (see `src/environments/environment.ts`).

### Running locally (full stack)

1. Start PostgreSQL, create/seed the `dispatchhub` database (see above).
2. `cd backend && mvn spring-boot:run` (leave running).
3. `cd frontend && npm install && npm start` (leave running).
4. Visit `http://localhost:4200`, register a new account or log in with a
   seeded user. If the backend `DataLoader` seeded the DB, use:
   - Admin: `admin@dispatchhub.com` / `Admin123!`
   - Rider: `rider1@dispatchhub.com` / `Rider123!`
   - Driver: `driver1@dispatchhub.com` / `Driver123!`
   If you loaded `seed-data/seed.sql` instead, all seeded users share the
   password `Password123!` (e.g. `admin@dispatchhub.com` / `Password123!`).

