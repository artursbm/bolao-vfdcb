package championship

import (
	"testing"
)

// newTestService creates a Service with default scoring (4/3/2/1) suitable for tests.
func newTestService(t *testing.T) *Service {
	t.Helper()
	return &Service{
		config: ScoringConfig{
			Exact:      4,
			WinnerDiff: 3,
			Winner:     2,
			Draw:       1,
		},
	}
}

func TestCalculatePoints_ExactScore(t *testing.T) {
	s := newTestService(t)

	// Exact home win
	if got := s.calculatePoints(2, 1, 2, 1); got != 4 {
		t.Errorf("exact home win: expected 4, got %d", got)
	}
	// Exact away win
	if got := s.calculatePoints(0, 3, 0, 3); got != 4 {
		t.Errorf("exact away win: expected 4, got %d", got)
	}
	// Exact score draw
	if got := s.calculatePoints(1, 1, 1, 1); got != 4 {
		t.Errorf("exact draw: expected 4, got %d", got)
	}
}

func TestCalculatePoints_CorrectWinnerAndGoalDiff(t *testing.T) {
	s := newTestService(t)

	// Guessed 3-2 (diff=1), result 2-1 (diff=1) → correct winner + same diff
	if got := s.calculatePoints(3, 2, 2, 1); got != 3 {
		t.Errorf("winner+diff home: expected 3, got %d", got)
	}
	// Guessed 0-2 (diff=-2), result 1-3 (diff=-2)
	if got := s.calculatePoints(0, 2, 1, 3); got != 3 {
		t.Errorf("winner+diff away: expected 3, got %d", got)
	}
}

func TestCalculatePoints_CorrectWinner(t *testing.T) {
	s := newTestService(t)

	// Guessed 1-0 (diff=1), result 3-1 (diff=2) → correct winner, different diff
	if got := s.calculatePoints(1, 0, 3, 1); got != 2 {
		t.Errorf("correct winner only: expected 2, got %d", got)
	}
}

func TestCalculatePoints_CorrectDraw(t *testing.T) {
	s := newTestService(t)

	// Guessed 1-1, result 2-2 → both draw but different scores
	if got := s.calculatePoints(1, 1, 2, 2); got != 1 {
		t.Errorf("correct draw: expected 1, got %d", got)
	}
}

func TestCalculatePoints_WrongGuess(t *testing.T) {
	s := newTestService(t)

	// Guessed home win, result away win
	if got := s.calculatePoints(2, 0, 0, 1); got != 0 {
		t.Errorf("wrong winner: expected 0, got %d", got)
	}
	// Guessed a draw, result was a home win
	if got := s.calculatePoints(1, 1, 2, 0); got != 0 {
		t.Errorf("wrong (draw vs win): expected 0, got %d", got)
	}
	// Guessed home win, result was a draw
	if got := s.calculatePoints(2, 1, 1, 1); got != 0 {
		t.Errorf("wrong (win vs draw): expected 0, got %d", got)
	}
}

func TestCalculatePoints_CustomScoring(t *testing.T) {
	s := &Service{
		config: ScoringConfig{Exact: 10, WinnerDiff: 7, Winner: 5, Draw: 3},
	}

	if got := s.calculatePoints(2, 1, 2, 1); got != 10 {
		t.Errorf("custom exact: expected 10, got %d", got)
	}
	if got := s.calculatePoints(1, 1, 0, 0); got != 3 {
		t.Errorf("custom draw: expected 3, got %d", got)
	}
}
