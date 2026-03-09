# Bolão VFDCB Service

A FIFA World Cup 2026 sweepstake backend service built in Go with Postgres and session-based authentication.

## Features

- User signup, login, and logout
- Session-based authentication with HMAC-SHA256 signed cookies
- PostgreSQL database with automatic migrations
- RESTful API with stdlib `http.ServeMux`

## Prerequisites

- Go 1.22+
- PostgreSQL 16+
- Docker (for integration tests)

## Quick Start

1. Set environment variables:

```bash
export DATABASE_URL="postgres://user:password@localhost:5432/bolao?sslmode=disable"
export COOKIE_HASH_KEY="your-32-byte-secret-key-here!!"
export SERVER_PORT="8080"
export SESSION_DURATION="720h"
```

2. Run the server:

```bash
make run
```

## API Endpoints

### Public Endpoints

- `POST /api/auth/signup` - Create an account
  ```json
  {
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123"
  }
  ```

- `POST /api/auth/login` - Authenticate
  ```json
  {
    "email": "john@example.com",
    "password": "password123"
  }
  ```

### Authenticated Endpoints

- `GET /api/auth/me` - Get current user info
- `POST /api/auth/logout` - Logout and clear session

## Development

```bash
# Run tests (unit tests only)
make test-short

# Run all tests including integration tests (requires Docker)
make test

# Build
make build
```

## Project Structure

```
├── cmd/server/         # Main application entrypoint
├── internal/
│   ├── auth/          # Authentication module
│   ├── config/        # Configuration
│   ├── database/      # Database connection
│   ├── migrations/    # SQL migrations
│   └── server/        # HTTP server and routing
```


## Data
Using football-data.org to fetch matches and results.