package auth

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

var (
	ErrInvalidCredentials = errors.New("invalid credentials")
	ErrEmailAlreadyExists = errors.New("email already exists")
	ErrSessionExpired     = errors.New("session expired")
)

type Service struct {
	repo            RepositoryInterface
	sessionDuration time.Duration
}

func NewService(repo RepositoryInterface, sessionDuration time.Duration) *Service {
	return &Service{
		repo:            repo,
		sessionDuration: sessionDuration,
	}
}

func (s *Service) Signup(ctx context.Context, name, email, password string) (*User, *Session, error) {
	// Check if user already exists
	existingUser, err := s.repo.GetUserByEmail(ctx, email)
	if err != nil && !errors.Is(err, ErrUserNotFound) {
		return nil, nil, fmt.Errorf("failed to check existing user: %w", err)
	}
	if existingUser != nil {
		return nil, nil, ErrEmailAlreadyExists
	}

	// Hash password
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to hash password: %w", err)
	}

	// Create user
	user, err := s.repo.CreateUser(ctx, name, email, string(hashedPassword))
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create user: %w", err)
	}

	// Create session
	expiresAt := time.Now().Add(s.sessionDuration)
	session, err := s.repo.CreateSession(ctx, user.ID, expiresAt)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create session: %w", err)
	}

	return user, session, nil
}

func (s *Service) Login(ctx context.Context, email, password string) (*User, *Session, error) {
	// Get user by email
	user, err := s.repo.GetUserByEmail(ctx, email)
	if err != nil {
		if errors.Is(err, ErrUserNotFound) {
			return nil, nil, ErrInvalidCredentials
		}
		return nil, nil, fmt.Errorf("failed to get user: %w", err)
	}

	// Verify password
	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password)); err != nil {
		return nil, nil, ErrInvalidCredentials
	}

	// Create session
	expiresAt := time.Now().Add(s.sessionDuration)
	session, err := s.repo.CreateSession(ctx, user.ID, expiresAt)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create session: %w", err)
	}

	return user, session, nil
}

func (s *Service) Logout(ctx context.Context, sessionID uuid.UUID) error {
	return s.repo.DeleteSession(ctx, sessionID)
}

func (s *Service) GetCurrentUser(ctx context.Context, sessionID uuid.UUID) (*User, error) {
	// Get session
	session, err := s.repo.GetSessionByID(ctx, sessionID)
	if err != nil {
		return nil, err
	}

	// Check if session is expired
	if time.Now().After(session.ExpiresAt) {
		// Clean up expired session
		_ = s.repo.DeleteSession(ctx, sessionID)
		return nil, ErrSessionExpired
	}

	// Get user
	user, err := s.repo.GetUserByID(ctx, session.UserID)
	if err != nil {
		return nil, err
	}

	return user, nil
}
