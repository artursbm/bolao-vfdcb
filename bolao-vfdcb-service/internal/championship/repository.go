package championship

import (
	"context"
	"errors"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

var (
	ErrMatchNotFound = errors.New("match not found")
	ErrGuessNotFound = errors.New("guess not found")
)

// RepositoryInterface defines the contract for championship persistence.
type RepositoryInterface interface {
	ListUpcomingMatches(ctx context.Context) ([]Match, error)
	ListAllMatches(ctx context.Context) ([]Match, error)
	GetMatchByID(ctx context.Context, id uuid.UUID) (*Match, error)
	UpsertGuess(ctx context.Context, userID, matchID uuid.UUID, homeScore, awayScore int) (*Guess, error)
	ListGuessesByUser(ctx context.Context, userID uuid.UUID) ([]GuessWithMatch, error)
	ListGuessesByMatch(ctx context.Context, matchID uuid.UUID) ([]Guess, error)
	UpdateMatchResult(ctx context.Context, matchID uuid.UUID, homeScore, awayScore int) (*Match, error)
	UpdateGuessPoints(ctx context.Context, guessID uuid.UUID, points int) error
}

// Repository implements RepositoryInterface against a PostgreSQL pool.
type Repository struct {
	pool *pgxpool.Pool
}

func NewRepository(pool *pgxpool.Pool) *Repository {
	return &Repository{pool: pool}
}

// scanMatch reads a single match row (with home/away team join) into a Match struct.
// Expected column order: m.id, ht.id, ht.name, ht.code, at.id, at.name, at.code,
//
//	m.match_time, m.home_score, m.away_score, m.status, m.created_at, m.updated_at
func scanMatch(row pgx.Row) (*Match, error) {
	m := &Match{}
	err := row.Scan(
		&m.ID,
		&m.HomeTeam.ID, &m.HomeTeam.Name, &m.HomeTeam.Code,
		&m.AwayTeam.ID, &m.AwayTeam.Name, &m.AwayTeam.Code,
		&m.MatchTime,
		&m.HomeScore, &m.AwayScore,
		&m.Status,
		&m.CreatedAt, &m.UpdatedAt,
	)
	if err != nil {
		return nil, err
	}
	return m, nil
}

const matchSelectCols = `
	m.id,
	ht.id, ht.name, ht.code,
	at.id, at.name, at.code,
	m.match_time,
	m.home_score, m.away_score,
	m.status,
	m.created_at, m.updated_at`

const matchJoins = `
	FROM matches m
	JOIN teams ht ON m.home_team_id = ht.id
	JOIN teams at ON m.away_team_id = at.id`

// ListUpcomingMatches returns SCHEDULED and IN_PROGRESS matches ordered by match_time.
func (r *Repository) ListUpcomingMatches(ctx context.Context) ([]Match, error) {
	query := "SELECT" + matchSelectCols + matchJoins + " WHERE m.status IN ('SCHEDULED','IN_PROGRESS') ORDER BY m.match_time ASC"
	rows, err := r.pool.Query(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("failed to list upcoming matches: %w", err)
	}
	defer rows.Close()

	return collectMatches(rows)
}

// ListAllMatches returns every match ordered by match_time.
func (r *Repository) ListAllMatches(ctx context.Context) ([]Match, error) {
	query := "SELECT" + matchSelectCols + matchJoins + " ORDER BY m.match_time ASC"

	rows, err := r.pool.Query(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("failed to list all matches: %w", err)
	}
	defer rows.Close()

	return collectMatches(rows)
}

func collectMatches(rows pgx.Rows) ([]Match, error) {
	var matches []Match
	for rows.Next() {
		m := Match{}
		err := rows.Scan(
			&m.ID,
			&m.HomeTeam.ID, &m.HomeTeam.Name, &m.HomeTeam.Code,
			&m.AwayTeam.ID, &m.AwayTeam.Name, &m.AwayTeam.Code,
			&m.MatchTime,
			&m.HomeScore, &m.AwayScore,
			&m.Status,
			&m.CreatedAt, &m.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan match: %w", err)
		}
		matches = append(matches, m)
	}
	return matches, rows.Err()
}

// GetMatchByID fetches a single match by its ID.
func (r *Repository) GetMatchByID(ctx context.Context, id uuid.UUID) (*Match, error) {
	query := "SELECT" + matchSelectCols + matchJoins + " WHERE m.id = $1"

	m, err := scanMatch(r.pool.QueryRow(ctx, query, id))
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrMatchNotFound
		}
		return nil, fmt.Errorf("failed to get match: %w", err)
	}
	return m, nil
}

// UpsertGuess inserts a new guess or updates an existing one for the same (user, match) pair.
func (r *Repository) UpsertGuess(ctx context.Context, userID, matchID uuid.UUID, homeScore, awayScore int) (*Guess, error) {
	query := `
		INSERT INTO guesses (user_id, match_id, home_score, away_score)
		VALUES ($1, $2, $3, $4)
		ON CONFLICT (user_id, match_id) DO UPDATE
			SET home_score = EXCLUDED.home_score,
			    away_score = EXCLUDED.away_score,
			    updated_at = CURRENT_TIMESTAMP
		RETURNING id, user_id, match_id, home_score, away_score, points, created_at, updated_at`

	g := &Guess{}
	err := r.pool.QueryRow(ctx, query, userID, matchID, homeScore, awayScore).Scan(
		&g.ID, &g.UserID, &g.MatchID,
		&g.HomeScore, &g.AwayScore,
		&g.Points,
		&g.CreatedAt, &g.UpdatedAt,
	)
	if err != nil {
		return nil, fmt.Errorf("failed to upsert guess: %w", err)
	}
	return g, nil
}

// ListGuessesByUser returns all matches ordered by match time, and includes the user's guess if it exists.
func (r *Repository) ListGuessesByUser(ctx context.Context, userID uuid.UUID) ([]GuessWithMatch, error) {
	query := `
		SELECT
			COALESCE(g.id, '00000000-0000-0000-0000-000000000000'),
			COALESCE(g.user_id, '00000000-0000-0000-0000-000000000000'),
			COALESCE(g.match_id, m.id),
			COALESCE(g.home_score, -1),
			COALESCE(g.away_score, -1),
			g.points,
			COALESCE(g.created_at, CURRENT_TIMESTAMP),
			COALESCE(g.updated_at, CURRENT_TIMESTAMP),` +
		matchSelectCols + `
		FROM matches m
		JOIN teams ht ON m.home_team_id = ht.id
		JOIN teams at ON m.away_team_id = at.id
		LEFT JOIN guesses g ON g.match_id = m.id AND g.user_id = $1
		ORDER BY m.match_time ASC`

	rows, err := r.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, fmt.Errorf("failed to list guesses by user: %w", err)
	}
	defer rows.Close()

	var results []GuessWithMatch
	for rows.Next() {
		gm := GuessWithMatch{}
		err := rows.Scan(
			&gm.Guess.ID, &gm.Guess.UserID, &gm.Guess.MatchID,
			&gm.Guess.HomeScore, &gm.Guess.AwayScore,
			&gm.Guess.Points,
			&gm.Guess.CreatedAt, &gm.Guess.UpdatedAt,
			// match fields
			&gm.Match.ID,
			&gm.Match.HomeTeam.ID, &gm.Match.HomeTeam.Name, &gm.Match.HomeTeam.Code,
			&gm.Match.AwayTeam.ID, &gm.Match.AwayTeam.Name, &gm.Match.AwayTeam.Code,
			&gm.Match.MatchTime,
			&gm.Match.HomeScore, &gm.Match.AwayScore,
			&gm.Match.Status,
			&gm.Match.CreatedAt, &gm.Match.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan guess: %w", err)
		}
		results = append(results, gm)
	}
	return results, rows.Err()
}

// ListGuessesByMatch returns all guesses for a given match (used for scoring after finalization).
func (r *Repository) ListGuessesByMatch(ctx context.Context, matchID uuid.UUID) ([]Guess, error) {
	query := `
		SELECT id, user_id, match_id, home_score, away_score, points, created_at, updated_at
		FROM guesses
		WHERE match_id = $1`

	rows, err := r.pool.Query(ctx, query, matchID)
	if err != nil {
		return nil, fmt.Errorf("failed to list guesses by match: %w", err)
	}
	defer rows.Close()

	var guesses []Guess
	for rows.Next() {
		g := Guess{}
		if err := rows.Scan(&g.ID, &g.UserID, &g.MatchID, &g.HomeScore, &g.AwayScore, &g.Points, &g.CreatedAt, &g.UpdatedAt); err != nil {
			return nil, fmt.Errorf("failed to scan guess: %w", err)
		}
		guesses = append(guesses, g)
	}
	return guesses, rows.Err()
}

// UpdateMatchResult sets the official score and marks the match as FINISHED.
func (r *Repository) UpdateMatchResult(ctx context.Context, matchID uuid.UUID, homeScore, awayScore int) (*Match, error) {
	query := `
		UPDATE matches
		SET home_score = $2, away_score = $3, status = 'FINISHED', updated_at = CURRENT_TIMESTAMP
		WHERE id = $1
		RETURNING id`

	var id uuid.UUID
	if err := r.pool.QueryRow(ctx, query, matchID, homeScore, awayScore).Scan(&id); err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrMatchNotFound
		}
		return nil, fmt.Errorf("failed to update match result: %w", err)
	}
	// Return the freshly updated match
	return r.GetMatchByID(ctx, id)
}

// UpdateGuessPoints writes the computed point total to a guess row.
func (r *Repository) UpdateGuessPoints(ctx context.Context, guessID uuid.UUID, points int) error {
	query := `UPDATE guesses SET points = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $1`
	_, err := r.pool.Exec(ctx, query, guessID, points)
	if err != nil {
		return fmt.Errorf("failed to update guess points: %w", err)
	}
	return nil
}
