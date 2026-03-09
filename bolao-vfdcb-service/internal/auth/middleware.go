package auth

import (
	"context"
	"errors"
	"log/slog"
	"net/http"

	"github.com/google/uuid"
)

type contextKey string

const (
	sessionIDKey contextKey = "session_id"
	userKey      contextKey = "user"

	// UserContextKey is the exported key used to store the authenticated *User
	// in the request context. Other packages should use this to retrieve the user.
	UserContextKey contextKey = "user"
)

type Middleware struct {
	service      *Service
	cookieSecret string
	logger       *slog.Logger
}

func NewMiddleware(service *Service, cookieSecret string, logger *slog.Logger) *Middleware {
	return &Middleware{
		service:      service,
		cookieSecret: cookieSecret,
		logger:       logger,
	}
}

func (m *Middleware) RequireAuth(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Get cookie
		cookie, err := r.Cookie(sessionCookieName)
		if err != nil {
			m.respondError(w, http.StatusUnauthorized, "Unauthorized")
			return
		}

		// Verify signature and extract session ID
		sessionIDStr, err := Verify(cookie.Value, m.cookieSecret)
		if err != nil {
			if errors.Is(err, ErrInvalidSignature) || errors.Is(err, ErrInvalidFormat) {
				m.respondError(w, http.StatusUnauthorized, "Invalid session")
				return
			}
			m.logger.Error("failed to verify cookie", "error", err)
			m.respondError(w, http.StatusInternalServerError, "Internal server error")
			return
		}

		// Parse session ID
		sessionID, err := uuid.Parse(sessionIDStr)
		if err != nil {
			m.respondError(w, http.StatusUnauthorized, "Invalid session")
			return
		}

		// Get current user
		user, err := m.service.GetCurrentUser(r.Context(), sessionID)
		if err != nil {
			if errors.Is(err, ErrSessionNotFound) || errors.Is(err, ErrSessionExpired) {
				m.respondError(w, http.StatusUnauthorized, "Session expired")
				return
			}
			m.logger.Error("failed to get current user", "error", err)
			m.respondError(w, http.StatusInternalServerError, "Internal server error")
			return
		}

		// Add session ID and user to context
		ctx := context.WithValue(r.Context(), sessionIDKey, sessionID)
		ctx = context.WithValue(ctx, userKey, user)

		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func (m *Middleware) respondError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	w.Write([]byte(`{"error":"` + message + `"}`))
}
