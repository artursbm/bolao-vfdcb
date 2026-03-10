package championship

import (
	"time"

	"github.com/google/uuid"
)

// MatchStatus represents the lifecycle state of a match.
type MatchStatus string

const (
	StatusScheduled  MatchStatus = "SCHEDULED"
	StatusInProgress MatchStatus = "IN_PROGRESS"
	StatusFinished   MatchStatus = "FINISHED"
)

// Team is a national team participating in the championship.
type Team struct {
	ID        uuid.UUID `json:"id"`
	Name      string    `json:"name"`
	Code      string    `json:"code"`
	CreatedAt time.Time `json:"created_at"`
}

// Match is a scheduled or completed game between two teams.
type Match struct {
	ID        uuid.UUID   `json:"id"`
	HomeTeam  Team        `json:"home_team"`
	AwayTeam  Team        `json:"away_team"`
	MatchTime time.Time   `json:"match_time"`
	HomeScore *int        `json:"home_score"` // nil until match is finished
	AwayScore *int        `json:"away_score"`
	Status    MatchStatus `json:"status"`
	CreatedAt time.Time   `json:"created_at"`
	UpdatedAt time.Time   `json:"updated_at"`
}

// Guess is a user's prediction for a match score.
type Guess struct {
	ID        uuid.UUID `json:"id"`
	UserID    uuid.UUID `json:"user_id"`
	MatchID   uuid.UUID `json:"match_id"`
	HomeScore int       `json:"home_score"`
	AwayScore int       `json:"away_score"`
	Points    *int      `json:"points"` // nil until match is scored
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// GuessWithMatch is a Guess enriched with the match it belongs to.
type GuessWithMatch struct {
	Guess
	Match Match `json:"match"`
}

// ScoringConfig holds the configurable point values for each scoring outcome.
type ScoringConfig struct {
	Exact      int
	WinnerDiff int
	Winner     int
	Draw       int
}

// UserRanking represents a user's accumulated points in the championship.
type UserRanking struct {
	UserID     uuid.UUID `json:"user_id"`
	UserName   string    `json:"user_name"`
	TotalScore int       `json:"total_score"`
}

