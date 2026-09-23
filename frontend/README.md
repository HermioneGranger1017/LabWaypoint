# LabWaypoint Frontend

Vue 3 + Vite application for the lab equipment workspace. The visual language
uses red Soviet constructivist posters: ink-black edges, paper surfaces, bold
numbering, diagonal geometry, and short hard shadows.

## Run locally

```sh
npm ci
npm run dev
```

Start the Spring Boot backend on `http://localhost:8080` first. Vite forwards
`/api` and `/uploads` to it. To use a different API origin, set `VITE_API_BASE`
in a local environment file; keep local environment files out of Git.
When deploying QR codes for phone scanning, set `VITE_PUBLIC_APP_URL` to the
frontend's public URL (for example, `https://lab.example`). A localhost URL
will only open on the computer that generated it. The scanner accepts links
from the current site and the configured public URL.

The login endpoint takes URL-encoded username and password parameters. Other
API requests send the returned JWT directly in the `Authorization` header.
The app expects the backend's `{ code, message, data }` response shape.

## Scope

The current UI covers authentication, equipment, borrowing, management,
reviews, tutorials, scanning, and a Wiki information-architecture page.
The Wiki has no article persistence API yet. Competition recommendations are
future work and are not shown as populated data.

`npm run build` writes to `dist/`. The build is not automatically copied to
`backend/src/main/resources/static/`.
