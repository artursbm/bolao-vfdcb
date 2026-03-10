package championship

import (
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"

	"github.com/go-playground/validator/v10"
	"github.com/google/uuid"

	"github.com/artursbm/bolao-vfdcb-service/internal/auth"
)

// Handler exposes championship endpoints.
type Handler struct {
	service  *Service
	validate *validator.Validate
	logger   *slog.Logger
}

func NewHandler(service *Service, logger *slog.Logger) *Handler {
	return &Handler{
		service:  service,
		validate: validator.New(),
		logger:   logger,
	}
}

// ── Request / Response types ──────────────────────────────────────────────────

type SubmitGuessRequest struct {
	MatchID   uuid.UUID `json:"match_id" validate:"required"`
	HomeScore int       `json:"home_score" validate:"min=0"`
	AwayScore int       `json:"away_score" validate:"min=0"`
}

type FinalizeMatchRequest struct {
	MatchID   uuid.UUID `json:"match_id" validate:"required"`
	HomeScore int       `json:"home_score" validate:"min=0"`
	AwayScore int       `json:"away_score" validate:"min=0"`
}

// ── Handlers ──────────────────────────────────────────────────────────────────

// GetMatches returns current and future matches for the homepage.
// GET /api/matches  (public)
func (h *Handler) GetMatches(w http.ResponseWriter, r *http.Request) {
	matches, err := h.service.ListUpcomingMatches(r.Context())
	if err != nil {
		h.logger.Error("failed to list matches", "error", err)
		h.respondError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	// Return an empty array instead of null
	if matches == nil {
		matches = []Match{}
	}
	h.respondJSON(w, http.StatusOK, matches)
}

// GetRanking returns the overall user ranking based on guess points.
// GET /api/ranking  (authenticated)
func (h *Handler) GetRanking(w http.ResponseWriter, r *http.Request) {
	ranking, err := h.service.GetRanking(r.Context())
	if err != nil {
		h.logger.Error("failed to get ranking", "error", err)
		h.respondError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	if ranking == nil {
		ranking = []UserRanking{}
	}
	h.respondJSON(w, http.StatusOK, ranking)
}

// GetGuesses returns all guesses the authenticated user has submitted.
// GET /api/guesses  (authenticated)
func (h *Handler) GetGuesses(w http.ResponseWriter, r *http.Request) {
	user, ok := r.Context().Value(auth.UserContextKey).(*auth.User)
	if !ok {
		h.respondError(w, http.StatusUnauthorized, "Unauthorized")
		return
	}

	guesses, err := h.service.GetUserGuesses(r.Context(), user.ID)
	if err != nil {
		h.logger.Error("failed to list guesses", "error", err, "user_id", user.ID)
		h.respondError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	if guesses == nil {
		guesses = []GuessWithMatch{}
	}
	h.respondJSON(w, http.StatusOK, guesses)
}

// SubmitGuess inserts or updates a guess for the authenticated user.
// POST /api/guesses  (authenticated)
func (h *Handler) SubmitGuess(w http.ResponseWriter, r *http.Request) {
	user, ok := r.Context().Value(auth.UserContextKey).(*auth.User)
	if !ok {
		h.respondError(w, http.StatusUnauthorized, "Unauthorized")
		return
	}

	var req SubmitGuessRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}
	if err := h.validate.Struct(req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Validation failed: "+err.Error())
		return
	}

	guess, err := h.service.SubmitGuess(r.Context(), user.ID, req.MatchID, req.HomeScore, req.AwayScore)
	if err != nil {
		switch {
		case errors.Is(err, ErrMatchNotFound):
			h.respondError(w, http.StatusNotFound, "Match not found")
		case errors.Is(err, ErrMatchAlreadyStarted):
			h.respondError(w, http.StatusUnprocessableEntity, "Match has already started, guesses are closed")
		default:
			h.logger.Error("failed to submit guess", "error", err)
			h.respondError(w, http.StatusInternalServerError, "Internal server error")
		}
		return
	}

	h.respondJSON(w, http.StatusOK, guess)
}

// FinalizeMatch sets the official result and triggers point calculation.
// POST /api/admin/match-results  (authenticated — admin only in the future)
func (h *Handler) FinalizeMatch(w http.ResponseWriter, r *http.Request) {
	var req FinalizeMatchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}
	if err := h.validate.Struct(req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Validation failed: "+err.Error())
		return
	}

	match, err := h.service.FinalizeMatch(r.Context(), req.MatchID, req.HomeScore, req.AwayScore)
	if err != nil {
		if errors.Is(err, ErrMatchNotFound) {
			h.respondError(w, http.StatusNotFound, "Match not found")
			return
		}
		h.logger.Error("failed to finalize match", "error", err)
		h.respondError(w, http.StatusInternalServerError, "Internal server error")
		return
	}

	h.respondJSON(w, http.StatusOK, match)
}

// ── Helpers ───────────────────────────────────────────────────────────────────

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
