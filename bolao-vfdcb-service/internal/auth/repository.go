package auth

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

var (
	ErrUserNotFound    = errors.New("user not found")
	ErrSessionNotFound = errors.New("session not found")
)

// RepositoryInterface defines the contract for user and session persistence
type RepositoryInterface interface {
	CreateUser(ctx context.Context, name, email, hashedPassword string) (*User, error)
	GetUserByEmail(ctx context.Context, email string) (*User, error)
	GetUserByID(ctx context.Context, id uuid.UUID) (*User, error)
	CreateSession(ctx context.Context, userID uuid.UUID, expiresAt time.Time) (*Session, error)
	GetSessionByID(ctx context.Context, id uuid.UUID) (*Session, error)
	DeleteSession(ctx context.Context, id uuid.UUID) error
	DeleteSessionsByUserID(ctx context.Context, userID uuid.UUID) error
}

type Repository struct {
	pool *pgxpool.Pool
}

func NewRepository(pool *pgxpool.Pool) *Repository {
	return &Repository{pool: pool}
}

func (r *Repository) CreateUser(ctx context.Context, name, email, hashedPassword string) (*User, error) {
	user := &User{}
	query := `
		INSERT INTO users (name, email, password)
		VALUES ($1, $2, $3)
		RETURNING id, name, email, password, created_at, updated_at
	`
	err := r.pool.QueryRow(ctx, query, name, email, hashedPassword).Scan(
		&user.ID, &user.Name, &user.Email, &user.Password, &user.CreatedAt, &user.UpdatedAt,
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create user: %w", err)
	}
	return user, nil
}

func (r *Repository) GetUserByEmail(ctx context.Context, email string) (*User, error) {
	user := &User{}
	query := `SELECT id, name, email, password, created_at, updated_at FROM users WHERE email = $1`
	err := r.pool.QueryRow(ctx, query, email).Scan(
		&user.ID, &user.Name, &user.Email, &user.Password, &user.CreatedAt, &user.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrUserNotFound
		}
		return nil, fmt.Errorf("failed to get user by email: %w", err)
	}
	return user, nil
}

func (r *Repository) GetUserByID(ctx context.Context, id uuid.UUID) (*User, error) {
	user := &User{}
	query := `SELECT id, name, email, password, created_at, updated_at FROM users WHERE id = $1`
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&user.ID, &user.Name, &user.Email, &user.Password, &user.CreatedAt, &user.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrUserNotFound
		}
		return nil, fmt.Errorf("failed to get user by id: %w", err)
	}
	return user, nil
}

func (r *Repository) CreateSession(ctx context.Context, userID uuid.UUID, expiresAt time.Time) (*Session, error) {
	session := &Session{}
	query := `
		INSERT INTO sessions (user_id, expires_at)
		VALUES ($1, $2)
		RETURNING id, user_id, expires_at, created_at
	`
	err := r.pool.QueryRow(ctx, query, userID, expiresAt).Scan(
		&session.ID, &session.UserID, &session.ExpiresAt, &session.CreatedAt,
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create session: %w", err)
	}
	return session, nil
}

func (r *Repository) GetSessionByID(ctx context.Context, id uuid.UUID) (*Session, error) {
	session := &Session{}
	query := `SELECT id, user_id, expires_at, created_at FROM sessions WHERE id = $1`
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&session.ID, &session.UserID, &session.ExpiresAt, &session.CreatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrSessionNotFound
		}
		return nil, fmt.Errorf("failed to get session: %w", err)
	}
	return session, nil
}

func (r *Repository) DeleteSession(ctx context.Context, id uuid.UUID) error {
	query := `DELETE FROM sessions WHERE id = $1`
	_, err := r.pool.Exec(ctx, query, id)
	if err != nil {
		return fmt.Errorf("failed to delete session: %w", err)
	}
	return nil
}

func (r *Repository) DeleteSessionsByUserID(ctx context.Context, userID uuid.UUID) error {
	query := `DELETE FROM sessions WHERE user_id = $1`
	_, err := r.pool.Exec(ctx, query, userID)
	if err != nil {
		return fmt.Errorf("failed to delete sessions by user id: %w", err)
	}
	return nil
}
