package championship

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
)

var (
	ErrMatchAlreadyStarted = errors.New("match has already started, guesses are closed")
	ErrMatchNotFinished    = errors.New("match is not finished yet")
)

// Service orchestrates championship business logic.
type Service struct {
	repo   RepositoryInterface
	config ScoringConfig
}

func NewService(repo RepositoryInterface, config ScoringConfig) *Service {
	return &Service{repo: repo, config: config}
}

// ListUpcomingMatches returns current and future matches for the homepage.
func (s *Service) ListUpcomingMatches(ctx context.Context) ([]Match, error) {
	return s.repo.ListUpcomingMatches(ctx)
}

// GetRanking returns the overall championship user ranking.
func (s *Service) GetRanking(ctx context.Context) ([]UserRanking, error) {
	return s.repo.GetRanking(ctx)
}

// SubmitGuess saves or updates a guess for the given match.
// Returns ErrMatchAlreadyStarted if match_time is in the past.
// TODO: verify when match started, if the guess was created before. If so should allow guess (race condition)
func (s *Service) SubmitGuess(ctx context.Context, userID, matchID uuid.UUID, homeScore, awayScore int) (*Guess, error) {
	match, err := s.repo.GetMatchByID(ctx, matchID)
	if err != nil {
		return nil, err
	}

	if !time.Now().Before(match.MatchTime) || match.Status != StatusScheduled {
		return nil, ErrMatchAlreadyStarted
	}

	return s.repo.UpsertGuess(ctx, userID, matchID, homeScore, awayScore)
}

// GetUserGuesses returns all guesses made by the given user, enriched with match data.
func (s *Service) GetUserGuesses(ctx context.Context, userID uuid.UUID) ([]GuessWithMatch, error) {
	return s.repo.ListGuessesByUser(ctx, userID)
}

// FinalizeMatch sets the official result for a match and calculates points for all guesses.
func (s *Service) FinalizeMatch(ctx context.Context, matchID uuid.UUID, homeScore, awayScore int) (*Match, error) {
	match, err := s.repo.UpdateMatchResult(ctx, matchID, homeScore, awayScore)
	if err != nil {
		return nil, fmt.Errorf("failed to update match result: %w", err)
	}

	guesses, err := s.repo.ListGuessesByMatch(ctx, matchID)
	if err != nil {
		return match, fmt.Errorf("match finalized but failed to load guesses for scoring: %w", err)
	}

	for _, g := range guesses {
		pts := s.calculatePoints(g.HomeScore, g.AwayScore, homeScore, awayScore)
		if err := s.repo.UpdateGuessPoints(ctx, g.ID, pts); err != nil {
			// Log-worthy but non-fatal; continue scoring other guesses
			_ = err
		}
	}

	return match, nil
}

// calculatePoints applies the configurable scoring rules.
//
//   - Exact score       → ScoreExact      (default 4)
//   - Right winner + right goal diff → ScoreWinnerDiff (default 3)
//   - Right winner only / right draw  → ScoreWinner / ScoreDraw (defaults 2 / 1)
//   - Wrong               → 0
func (s *Service) calculatePoints(guessHome, guessAway, realHome, realAway int) int {
	// Exact score
	if guessHome == realHome && guessAway == realAway {
		return s.config.Exact
	}

	guessDiff := guessHome - guessAway
	realDiff := realHome - realAway

	switch {
	case realDiff == 0:
		// It was a draw — reward guessing a draw, different scores
		if guessDiff == 0 {
			return s.config.Draw
		}
		return 0

	default:
		// There was a winner
		guessWinner := sign(guessHome - guessAway)
		realWinner := sign(realHome - realAway)

		if guessWinner != realWinner {
			return 0
		}
		// Correct winner: check goal difference
		if guessDiff == realDiff {
			return s.config.WinnerDiff
		}
		return s.config.Winner
	}
}

func sign(n int) int {
	if n > 0 {
		return 1
	}
	if n < 0 {
		return -1
	}
	return 0
}
