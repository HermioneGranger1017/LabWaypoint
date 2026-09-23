# LabWaypoint

A platform for lab equipment, competition discovery, and project readiness.

## Current status

The `backend/` directory contains the Spring Boot API for lab equipment, users,
registration review, instructions, edit approvals, borrowing, and image uploads.
The `frontend/` directory contains the Vue 3 reconstruction of the lab workspace.
Competition discovery and recommendations are planned; they have no backend API yet.

## Backend setup

- Java 17, Maven, and MySQL are required.
- Copy `backend/src/main/resources/application.yml.example` to
  `backend/src/main/resources/application.yml`.
- Set `DB_PASSWORD` to your local database password and `JWT_SECRET` to a private,
  randomly generated value of at least 32 characters. Keep both values outside Git.
- Set `UPLOAD_PATH` if you want files stored outside `./uploads/`.
- Use an existing `lab_equipment` database. `backend/sql/` contains two migrations
  for the borrowing and registration review features; a complete fresh database
  schema is not yet included.

From `backend/`, run `mvn spring-boot:run` after configuring the database.

## Frontend development

From `frontend/`, run `npm ci` and `npm run dev`. Vite serves the app on port
5173 and proxies `/api` and `/uploads` to the Spring Boot server on port 8080.
The frontend uses hash routes, so its home URL is `http://localhost:5173/#/home`.
Run `npm run build` to create `frontend/dist/`; this command does not copy files
into the backend's static directory.

## Repository layout

- `backend/src/main/java/`: Spring Boot API and business logic
- `backend/src/main/resources/application.yml.example`: safe configuration template
- `backend/sql/`: existing database migration scripts
- `frontend/src/`: Vue 3 pages, API client, authentication state, and poster-style UI

Local credentials, uploaded files, build output, and the previous frontend bundle
are excluded from this repository.
