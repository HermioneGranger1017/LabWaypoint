# LabWaypoint

A platform for lab equipment, competition discovery, and project readiness.

## Current status

The `backend/` directory contains the Spring Boot API for lab equipment, users,
registration review, instructions, edit approvals, borrowing, and image uploads.
Competition discovery and recommendations are planned. The frontend will be added
later.

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

## Repository layout

- `backend/src/main/java/`: Spring Boot API and business logic
- `backend/src/main/resources/application.yml.example`: safe configuration template
- `backend/sql/`: existing database migration scripts

Local credentials, uploaded files, build output, and the previous frontend bundle
are excluded from this repository.
