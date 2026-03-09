package auth

import (
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"

	"github.com/go-playground/validator/v10"
	"github.com/google/uuid"
)

const sessionCookieName = "session"

type Handler struct {
	service      *Service
	cookieSecret string
	validate     *validator.Validate
	logger       *slog.Logger
}

func NewHandler(service *Service, cookieSecret string, logger *slog.Logger) *Handler {
	return &Handler{
		service:      service,
		cookieSecret: cookieSecret,
		validate:     validator.New(),
		logger:       logger,
	}
}

type SignupRequest struct {
	Name     string `json:"name" validate:"required,min=2,max=100"`
	Email    string `json:"email" validate:"required,email"`
	Password string `json:"password" validate:"required,min=8"`
}

type LoginRequest struct {
	Email    string `json:"email" validate:"required,email"`
	Password string `json:"password" validate:"required"`
}

type UserResponse struct {
	ID    uuid.UUID `json:"id"`
	Name  string    `json:"name"`
	Email string    `json:"email"`
}

func (h *Handler) Signup(w http.ResponseWriter, r *http.Request) {
	var req SignupRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	if err := h.validate.Struct(req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Validation failed: "+err.Error())
		return
	}

	user, session, err := h.service.Signup(r.Context(), req.Name, req.Email, req.Password)
	if err != nil {
		if errors.Is(err, ErrEmailAlreadyExists) {
			h.respondError(w, http.StatusConflict, "Email already exists")
			return
		}
		h.logger.Error("signup failed", "error", err)
		h.respondError(w, http.StatusInternalServerError, "Internal server error")
		return
	}

	// Set session cookie
	h.setSessionCookie(w, session.ID)

	h.respondJSON(w, http.StatusCreated, UserResponse{
		ID:    user.ID,
		Name:  user.Name,
		Email: user.Email,
	})
}

func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	var req LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	if err := h.validate.Struct(req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Validation failed: "+err.Error())
		return
	}

	user, session, err := h.service.Login(r.Context(), req.Email, req.Password)
	if err != nil {
		if errors.Is(err, ErrInvalidCredentials) {
			h.respondError(w, http.StatusUnauthorized, "Invalid credentials")
			return
		}
		h.logger.Error("login failed", "error", err)
		h.respondError(w, http.StatusInternalServerError, "Internal server error")
		return
	}

	// Set session cookie
	h.setSessionCookie(w, session.ID)

	h.respondJSON(w, http.StatusOK, UserResponse{
		ID:    user.ID,
		Name:  user.Name,
		Email: user.Email,
	})
}

func (h *Handler) Logout(w http.ResponseWriter, r *http.Request) {
	// Get session ID from context (set by middleware)
	sessionID, ok := r.Context().Value(sessionIDKey).(uuid.UUID)
	if !ok {
		h.respondError(w, http.StatusUnauthorized, "Unauthorized")
		return
	}

	if err := h.service.Logout(r.Context(), sessionID); err != nil {
		h.logger.Error("logout failed", "error", err)
		h.respondError(w, http.StatusInternalServerError, "Internal server error")
		return
	}

	// Clear cookie
	http.SetCookie(w, &http.Cookie{
		Name:     sessionCookieName,
		Value:    "",
		Path:     "/",
		MaxAge:   -1,
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteLaxMode,
	})

	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) Me(w http.ResponseWriter, r *http.Request) {
	// Get user from context (set by middleware)
	user, ok := r.Context().Value(userKey).(*User)
	if !ok {
		h.respondError(w, http.StatusUnauthorized, "Unauthorized")
		return
	}

	h.respondJSON(w, http.StatusOK, UserResponse{
		ID:    user.ID,
		Name:  user.Name,
		Email: user.Email,
	})
}

func (h *Handler) setSessionCookie(w http.ResponseWriter, sessionID uuid.UUID) {
	signedValue := Sign(sessionID.String(), h.cookieSecret)
	http.SetCookie(w, &http.Cookie{
		Name:     sessionCookieName,
		Value:    signedValue,
		Path:     "/",
		MaxAge:   int(h.service.sessionDuration.Seconds()),
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteLaxMode,
	})
}

func (h *Handler) respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(data); err != nil {
		h.logger.Error("failed to encode JSON response", "error", err)
	}
}

func (h *Handler) respondError(w http.ResponseWriter, status int, message string) {
	h.respondJSON(w, status, map[string]string{"error": message})
}
