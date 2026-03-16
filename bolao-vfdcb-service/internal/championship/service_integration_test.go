package championship

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestIntegrationService_ListUpcomingMatches(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test")
	}

	repo := NewRepository(testPool)
	service := NewService(repo, ScoringConfig{Exact: 4, WinnerDiff: 3, Winner: 2, Draw: 1})
	ctx := context.Background()

	// Setup teams
	teamA := uuid.New()
	teamB := uuid.New()
	_, _ = testPool.Exec(ctx, "INSERT INTO teams (id, name, code) VALUES ($1, 'Team A', 'TMA'), ($2, 'Team B', 'TMB')", teamA, teamB)

	now := time.Now()

	// Insert matches with different statuses
	match1 := uuid.New() // SCHEDULED
	match2 := uuid.New() // IN_PROGRESS
	match3 := uuid.New() // FINISHED
	
	_, err := testPool.Exec(ctx, `
		INSERT INTO matches (id, home_team_id, away_team_id, match_time, status) VALUES 
		($1, $4, $5, $6, 'SCHEDULED'),
		($2, $5, $4, $7, 'IN_PROGRESS'),
		($3, $4, $5, $8, 'FINISHED')`,
		match1, match2, match3, teamA, teamB, now.Add(time.Hour), now.Add(-time.Hour), now.Add(-2*time.Hour))
	require.NoError(t, err)

	matches, err := service.ListUpcomingMatches(ctx)
	require.NoError(t, err)

	// Should only return SCHEDULED and IN_PROGRESS matches
	// Note: We don't check for exact length because migrations might seed other matches
	
	found1, found2 := false, false
	for _, m := range matches {
		if m.ID == match1 {
			found1 = true
			assert.Equal(t, StatusScheduled, m.Status)
		}
		if m.ID == match2 {
			found2 = true
			assert.Equal(t, StatusInProgress, m.Status)
		}
		assert.NotEqual(t, match3, m.ID)
	}
	assert.True(t, found1)
	assert.True(t, found2)
}

func TestIntegrationService_GetUserGuesses(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test")
	}

	repo := NewRepository(testPool)
	service := NewService(repo, ScoringConfig{Exact: 4, WinnerDiff: 3, Winner: 2, Draw: 1})
	ctx := context.Background()

	// Setup
	userID := uuid.New()
	_, _ = testPool.Exec(ctx, "INSERT INTO users (id, name, email, password) VALUES ($1, 'Test User', 'test@user.com', 'pass')", userID)

	teamA := uuid.New()
	teamB := uuid.New()
	_, _ = testPool.Exec(ctx, "INSERT INTO teams (id, name, code) VALUES ($1, 'Team C', 'TMC'), ($2, 'Team D', 'TMD')", teamA, teamB)

	match1 := uuid.New()
	match2 := uuid.New()
	now := time.Now()
	_, err := testPool.Exec(ctx, "INSERT INTO matches (id, home_team_id, away_team_id, match_time, status) VALUES ($1, $2, $3, $4, 'SCHEDULED')", match1, teamA, teamB, now.Add(time.Hour))
	require.NoError(t, err)
	_, err = testPool.Exec(ctx, "INSERT INTO matches (id, home_team_id, away_team_id, match_time, status) VALUES ($1, $2, $3, $4, 'SCHEDULED')", match2, teamB, teamA, now.Add(2*time.Hour))
	require.NoError(t, err)

	// User guesses on match 1 only
	_, err = service.SubmitGuess(ctx, userID, match1, 2, 1)
	require.NoError(t, err)

	results, err := service.GetUserGuesses(ctx, userID)
	require.NoError(t, err)

	// Expect the user's specific guesses
	// Note: We don't check for exact length because migrations might seed other matches

	found1, found2 := false, false
	for _, r := range results {
		if r.Match.ID == match1 {
			found1 = true
			assert.Equal(t, 2, r.Guess.HomeScore)
			assert.Equal(t, 1, r.Guess.AwayScore)
			assert.NotEqual(t, uuid.Nil, r.Guess.ID)
		}
		if r.Match.ID == match2 {
			found2 = true
			assert.Equal(t, -1, r.Guess.HomeScore) // COALESCE(g.home_score, -1) in repo
			assert.Equal(t, -1, r.Guess.AwayScore)
		}
	}
	assert.True(t, found1)
	assert.True(t, found2)
}

func TestIntegrationService_SubmitGuess(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test")
	}

	repo := NewRepository(testPool)
	service := NewService(repo, ScoringConfig{Exact: 4, WinnerDiff: 3, Winner: 2, Draw: 1})
	ctx := context.Background()

	// Setup
	userID := uuid.New()
	_, _ = testPool.Exec(ctx, "INSERT INTO users (id, name, email, password) VALUES ($1, 'Guess User', 'guess@user.com', 'pass')", userID)

	teamA := uuid.New()
	teamB := uuid.New()
	_, _ = testPool.Exec(ctx, "INSERT INTO teams (id, name, code) VALUES ($1, 'Team E', 'TME'), ($2, 'Team F', 'TMF')", teamA, teamB)

	now := time.Now()
	matchFuture := uuid.New()
	matchPast := uuid.New()
	_, err := testPool.Exec(ctx, "INSERT INTO matches (id, home_team_id, away_team_id, match_time, status) VALUES ($1, $2, $3, $4, 'SCHEDULED')", matchFuture, teamA, teamB, now.Add(time.Hour))
	require.NoError(t, err)
	_, err = testPool.Exec(ctx, "INSERT INTO matches (id, home_team_id, away_team_id, match_time, status) VALUES ($1, $2, $3, $4, 'SCHEDULED')", matchPast, teamA, teamB, now.Add(-time.Second))
	require.NoError(t, err)

	// Scenario A: Valid Guess
	guess, err := service.SubmitGuess(ctx, userID, matchFuture, 1, 0)
	assert.NoError(t, err)
	assert.NotNil(t, guess)
	assert.Equal(t, 1, guess.HomeScore)

	// Scenario B: Update Guess
	guessUpdated, err := service.SubmitGuess(ctx, userID, matchFuture, 3, 3)
	assert.NoError(t, err)
	assert.Equal(t, guess.ID, guessUpdated.ID)
	assert.Equal(t, 3, guessUpdated.HomeScore)

	// Scenario C: Past Match
	_, err = service.SubmitGuess(ctx, userID, matchPast, 1, 1)
	assert.ErrorIs(t, err, ErrMatchAlreadyStarted)
}

func TestIntegrationService_FinalizeMatch(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test")
	}

	repo := NewRepository(testPool)
	service := NewService(repo, ScoringConfig{Exact: 4, WinnerDiff: 3, Winner: 2, Draw: 1})
	ctx := context.Background()

	// Setup
	teamA := uuid.New()
	teamB := uuid.New()
	_, _ = testPool.Exec(ctx, "INSERT INTO teams (id, name, code) VALUES ($1, 'Team G', 'TMG'), ($2, 'Team H', 'TMH')", teamA, teamB)

	matchID := uuid.New()
	now := time.Now()
	_, err := testPool.Exec(ctx, "INSERT INTO matches (id, home_team_id, away_team_id, match_time, status) VALUES ($1, $2, $3, $4, 'SCHEDULED')", matchID, teamA, teamB, now.Add(time.Hour))
	require.NoError(t, err)

	users := []struct {
		id    uuid.UUID
		home  int
		away  int
		score int
	}{
		{uuid.New(), 2, 1, 4}, // Exact
		{uuid.New(), 3, 2, 3}, // Winner + Diff
		{uuid.New(), 3, 0, 2}, // Winner only (diff 3 vs 1)
		{uuid.New(), 1, 1, 0}, // Wrong
	}

	for i, u := range users {
		_, _ = testPool.Exec(ctx, "INSERT INTO users (id, name, email, password) VALUES ($1, $2, $3, 'pass')", u.id, fmt.Sprintf("User %d", i), fmt.Sprintf("u%d@test.com", i))
		_, err = service.SubmitGuess(ctx, u.id, matchID, u.home, u.away)
		// We need to override the "match already started" check if time.Now() moves fast, 
		// but match is in future so it's fine. 
		// HOWEVER, SubmitGuess doesn't allow guessing if it's NOT SCHEDULED or in past.
		// Wait, I just realized SubmitGuess has the check. I'll just use the match I created.
		require.NoError(t, err)
	}

	// Finalize match with score 2-1
	updatedMatch, err := service.FinalizeMatch(ctx, matchID, 2, 1)
	require.NoError(t, err)
	assert.Equal(t, StatusFinished, updatedMatch.Status)
	assert.Equal(t, 2, *updatedMatch.HomeScore)

	// Verify points
	guesses, err := repo.ListGuessesByMatch(ctx, matchID)
	require.NoError(t, err)
	assert.Len(t, guesses, 4)

	for _, g := range guesses {
		for _, u := range users {
			if g.UserID == u.id {
				require.NotNil(t, g.Points)
				assert.Equal(t, u.score, *g.Points, "User with guess %d-%d should have %d points", u.home, u.away, u.score)
			}
		}
	}
}
