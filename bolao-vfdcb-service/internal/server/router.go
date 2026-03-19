package server

import (
	"log/slog"
	"net/http"
	"slices"
	"time"

	"github.com/artursbm/bolao-vfdcb-service/internal/auth"
	"github.com/artursbm/bolao-vfdcb-service/internal/championship"
)

func NewRouter(
	authHandler *auth.Handler,
	authMiddleware *auth.Middleware,
	champHandler *championship.Handler,
	logger *slog.Logger,
	allowedOrigins []string,
) http.Handler {
	mux := http.NewServeMux()

	// ── Auth routes ──────────────────────────────────────────────────────────
	// Public
	mux.HandleFunc("POST /api/auth/signup", authHandler.Signup)
	mux.HandleFunc("POST /api/auth/login", authHandler.Login)

	// Authenticated
	mux.Handle("POST /api/auth/logout", authMiddleware.RequireAuth(http.HandlerFunc(authHandler.Logout)))
	mux.Handle("GET /api/auth/me", authMiddleware.RequireAuth(http.HandlerFunc(authHandler.Me)))

	// ── Championship routes ──────────────────────────────────────────────────
	// Public — list upcoming matches for the homepage
	mux.HandleFunc("GET /api/matches", champHandler.GetMatches)

	// Authenticated — user guesses & rankings
	mux.Handle("GET /api/guesses", authMiddleware.RequireAuth(http.HandlerFunc(champHandler.GetGuesses)))
	mux.Handle("POST /api/guesses", authMiddleware.RequireAuth(http.HandlerFunc(champHandler.SubmitGuess)))
	mux.Handle("GET /api/ranking", authMiddleware.RequireAuth(http.HandlerFunc(champHandler.GetRanking)))

	// Admin — finalize a match and trigger scoring
	mux.Handle("POST /api/admin/match-results", authMiddleware.RequireAuth(http.HandlerFunc(champHandler.FinalizeMatch)))

	// Apply global middleware
	handler := loggingMiddleware(logger)(mux)
	handler = recoveryMiddleware(logger)(handler)
	handler = corsMiddleware(allowedOrigins)(handler)

	return handler
}

// loggingMiddleware logs incoming requests
func loggingMiddleware(logger *slog.Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()
			logger.Info("request started",
				"method", r.Method,
				"path", r.URL.Path,
				"remote_addr", r.RemoteAddr,
			)
			next.ServeHTTP(w, r)
			logger.Info("request completed",
				"method", r.Method,
				"path", r.URL.Path,
				"duration_ms", time.Since(start).Milliseconds(),
			)
		})
	}
}

// recoveryMiddleware recovers from panics
func recoveryMiddleware(logger *slog.Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			defer func() {
				if err := recover(); err != nil {
					logger.Error("panic recovered",
						"error", err,
						"method", r.Method,
						"path", r.URL.Path,
					)
					w.Header().Set("Content-Type", "application/json")
					w.WriteHeader(http.StatusInternalServerError)
					w.Write([]byte(`{"error":"Internal server error"}`))
				}
			}()
			next.ServeHTTP(w, r)
		})
	}
}

// corsMiddleware adds CORS headers
func corsMiddleware(allowedOrigins []string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			origin := r.Header.Get("Origin")
			if origin != "" {
				if slices.Contains(allowedOrigins, origin) || slices.Contains(allowedOrigins, "*") {
					w.Header().Set("Access-Control-Allow-Origin", origin)
				}
			}

			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
			w.Header().Set("Access-Control-Allow-Credentials", "true")

			if r.Method == http.MethodOptions {
				w.WriteHeader(http.StatusOK)
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}
