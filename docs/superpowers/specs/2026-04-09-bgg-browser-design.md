# BGG Game Browser — Design Spec
_Date: 2026-04-09_

## Overview

A mobile-friendly web application for browsing BoardGameGeek (BGG) games by rank, score, complexity, player count, and name. Read-only, no user accounts. The backend proxies and caches BGG data locally for fast filtering and sorting.

---

## Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + Vite + TypeScript + Tailwind CSS |
| Backend | Spring Boot 3 (Java 21) |
| Database | PostgreSQL (hosted on Railway) |
| Frontend hosting | Vercel (free) |
| Backend hosting | Railway (free tier) |
| CI/CD | GitHub Actions |

---

## Architecture

```
GitHub Monorepo
├── /frontend   (React + Vite SPA)
└── /backend    (Spring Boot REST API)

BGG XML API2 ──(weekly sync job)──→ PostgreSQL on Railway
                                          ↑
Browser / Phone ──→ Vercel ──→ Railway REST API
```

- The React SPA is a static build deployed to Vercel.
- The Spring Boot backend exposes a REST API consumed by the frontend.
- PostgreSQL lives on Railway alongside the backend.
- A scheduled Spring Boot job (`@Scheduled`, weekly) pages through the BGG XML API2, fetches the top ~5,000 games by rank, and upserts them into Postgres. The first sync runs on application startup.
- CORS on the backend is restricted to the Vercel production domain (and localhost for development).

---

## Data Model

```sql
CREATE TABLE games (
    id               BIGINT PRIMARY KEY,   -- BGG game ID
    name             TEXT NOT NULL,
    year_published   INT,
    bgg_rank         INT,
    avg_rating       NUMERIC(4,2),         -- BGG score 1–10
    complexity       NUMERIC(3,2),         -- BGG weight 1–5
    min_players      INT,
    max_players      INT,
    thumbnail_url    TEXT,
    description      TEXT,
    play_time_min    INT,                  -- minutes
    play_time_max    INT,                  -- minutes
    last_synced_at   TIMESTAMP NOT NULL
);

CREATE INDEX ON games (bgg_rank);
CREATE INDEX ON games (avg_rating);
CREATE INDEX ON games (complexity);
```

---

## Backend API

### `GET /api/games`

Returns a paginated, filtered, sorted list of games.

**Query parameters:**

| Parameter | Type | Description |
|---|---|---|
| `page` | int | Page number (0-based, default 0) |
| `size` | int | Page size (default 20, max 50) |
| `sort` | string | `rank` \| `rating` \| `complexity` \| `name` (default `rank`) |
| `name` | string | Case-insensitive name search |
| `minPlayers` | int | Minimum player count filter |
| `maxPlayers` | int | Maximum player count filter |
| `minComplexity` | decimal | Minimum complexity (1.0–5.0) |
| `maxComplexity` | decimal | Maximum complexity (1.0–5.0) |
| `maxRank` | int | Only show games ranked at or above this (e.g. 500 = top 500) |
| `sortDir` | string | `asc` \| `desc` (default `asc` for name, `asc` for rank, `desc` for rating) |

**Response:**
```json
{
  "content": [
    {
      "id": 224517,
      "name": "Brass: Birmingham",
      "yearPublished": 2018,
      "bggRank": 1,
      "avgRating": 8.61,
      "complexity": 3.89,
      "minPlayers": 2,
      "maxPlayers": 4,
      "thumbnailUrl": "https://..."
    }
  ],
  "totalElements": 312,
  "totalPages": 16,
  "number": 0,
  "size": 20
}
```

### `GET /api/games/{id}`

Returns full details for a single game including description.

---

## UI Design

### Layout (desktop)

```
┌──────────────────────────────────────────────────┐
│  Nav: BGG Browser                                │
├──────────────┬───────────────────────────────────┤
│              │                                   │
│   FILTERS    │   Game list (paginated)           │
│              │                                   │
│  Name search │   [thumbnail] Name        #1      │
│  Players 2–4 │   ⭐ 8.6  ★★★★☆  2–4p           │
│  Complexity  │                                   │
│  Rank range  │   [thumbnail] Name        #2      │
│              │   ⭐ 8.5  ★★★☆☆  2–4p           │
│  [Apply]     │                                   │
│  [Reset]     │   …                               │
│              │                                   │
└──────────────┴───────────────────────────────────┘
```

- On mobile, the sidebar collapses behind a "Filters" button that opens a full-screen drawer.
- The game list shows: thumbnail, name, BGG rank, score, complexity (star visual), player count.
- Pagination controls at the bottom of the list.

### Game Detail Page

Clicking a game navigates to `/games/{id}`. Shows:
- Thumbnail image
- Name, year published
- BGG rank and score
- Complexity (numeric + visual)
- Player count and play time
- Description
- "View on BGG" link (external)
- Back button returns to the list, preserving filter/page state (filters are stored in URL query parameters, so the browser back button restores them automatically).

---

## CI/CD Pipeline

### Repository structure

```
github.com/<user>/bgg-browser
├── .github/workflows/
│   ├── ci.yml       # runs on every PR
│   └── cd.yml       # runs on merge to main
├── frontend/
└── backend/
```

### CI (on every PR)

1. **Backend**: `mvn test` — Spring Boot unit tests must pass
2. **Frontend**: `npm run build` + `npm run lint` — build and lint must pass
3. Both jobs must succeed before merge is allowed (branch protection rule on `main`)

### CD (on merge to main)

1. **Frontend**: Vercel auto-deploys on push to `main` via GitHub integration
2. **Backend**: GitHub Actions builds a Docker image, pushes to Railway via Railway CLI

### Preview environments

Every PR automatically gets a Vercel preview deployment at a unique URL — useful for reviewing UI changes before merge.

### Secrets

| Secret | Stored in |
|---|---|
| `RAILWAY_TOKEN` | GitHub Actions secret |
| `VERCEL_TOKEN` | GitHub Actions secret (or Vercel GitHub integration) |
| `DATABASE_URL` | Railway environment variable |
| `BGG_SYNC_*` config | Railway environment variable |

Nothing sensitive is committed to the repository.

---

## Security

- CORS restricted to the Vercel production domain + `localhost` (dev only)
- Rate limiting on `/api/games` via Spring Boot (e.g. Bucket4j or simple request throttle)
- All secrets injected via environment variables, never in code
- `main` branch protected: PRs required, CI must pass before merge
- No user input reaches the database directly — all filtering done via Spring Data JPA query parameters (SQL injection not possible)

---

## Out of Scope

- User accounts, authentication, or saved state
- Android app / native mobile
- BGG user collection import
- Game categories, mechanics, or designer filtering (can be added later)
- Admin UI for triggering manual syncs
