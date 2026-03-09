package auth

import (
	"context"
	"testing"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

// MockRepository implements a simple in-memory repository for testing
type MockRepository struct {
	users    map[string]*User
	sessions map[uuid.UUID]*Session
}

func NewMockRepository() *MockRepository {
	return &MockRepository{
		users:    make(map[string]*User),
		sessions: make(map[uuid.UUID]*Session),
	}
}

func (m *MockRepository) CreateUser(ctx context.Context, name, email, hashedPassword string) (*User, error) {
	if _, exists := m.users[email]; exists {
		return nil, ErrEmailAlreadyExists
	}
	user := &User{
		ID:        uuid.New(),
		Name:      name,
		Email:     email,
		Password:  hashedPassword,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}
	m.users[email] = user
	return user, nil
}

func (m *MockRepository) GetUserByEmail(ctx context.Context, email string) (*User, error) {
	user, exists := m.users[email]
	if !exists {
		return nil, ErrUserNotFound
	}
	return user, nil
}

func (m *MockRepository) GetUserByID(ctx context.Context, id uuid.UUID) (*User, error) {
	for _, user := range m.users {
		if user.ID == id {
			return user, nil
		}
	}
	return nil, ErrUserNotFound
}

func (m *MockRepository) CreateSession(ctx context.Context, userID uuid.UUID, expiresAt time.Time) (*Session, error) {
	session := &Session{
		ID:        uuid.New(),
		UserID:    userID,
		ExpiresAt: expiresAt,
		CreatedAt: time.Now(),
	}
	m.sessions[session.ID] = session
	return session, nil
}

func (m *MockRepository) GetSessionByID(ctx context.Context, id uuid.UUID) (*Session, error) {
	session, exists := m.sessions[id]
	if !exists {
		return nil, ErrSessionNotFound
	}
	return session, nil
}

func (m *MockRepository) DeleteSession(ctx context.Context, id uuid.UUID) error {
	delete(m.sessions, id)
	return nil
}

func (m *MockRepository) DeleteSessionsByUserID(ctx context.Context, userID uuid.UUID) error {
	for id, session := range m.sessions {
		if session.UserID == userID {
			delete(m.sessions, id)
		}
	}
	return nil
}

func TestSignup(t *testing.T) {
	repo := NewMockRepository()
	service := NewService(repo, 24*time.Hour)

	user, session, err := service.Signup(context.Background(), "Test User", "test@example.com", "password123")
	if err != nil {
		t.Fatalf("Signup failed: %v", err)
	}

	if user.Name != "Test User" {
		t.Errorf("Expected name 'Test User', got %s", user.Name)
	}
	if user.Email != "test@example.com" {
		t.Errorf("Expected email 'test@example.com', got %s", user.Email)
	}

	// Verify password is hashed
	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte("password123")); err != nil {
		t.Error("Password was not properly hashed")
	}

	if session == nil {
		t.Error("Session was not created")
	}
}

func TestSignupDuplicateEmail(t *testing.T) {
	repo := NewMockRepository()
	service := NewService(repo, 24*time.Hour)

	_, _, err := service.Signup(context.Background(), "User 1", "test@example.com", "password123")
	if err != nil {
		t.Fatalf("First signup failed: %v", err)
	}

	_, _, err = service.Signup(context.Background(), "User 2", "test@example.com", "password456")
	if err != ErrEmailAlreadyExists {
		t.Errorf("Expected ErrEmailAlreadyExists, got %v", err)
	}
}

func TestLogin(t *testing.T) {
	repo := NewMockRepository()
	service := NewService(repo, 24*time.Hour)

	// Create user first
	_, _, err := service.Signup(context.Background(), "Test User", "test@example.com", "password123")
	if err != nil {
		t.Fatalf("Signup failed: %v", err)
	}

	// Test login
	user, session, err := service.Login(context.Background(), "test@example.com", "password123")
	if err != nil {
		t.Fatalf("Login failed: %v", err)
	}

	if user.Email != "test@example.com" {
		t.Errorf("Expected email 'test@example.com', got %s", user.Email)
	}
	if session == nil {
		t.Error("Session was not created")
	}
}

func TestLoginWrongPassword(t *testing.T) {
	repo := NewMockRepository()
	service := NewService(repo, 24*time.Hour)

	// Create user first
	_, _, err := service.Signup(context.Background(), "Test User", "test@example.com", "password123")
	if err != nil {
		t.Fatalf("Signup failed: %v", err)
	}

	// Test login with wrong password
	_, _, err = service.Login(context.Background(), "test@example.com", "wrongpassword")
	if err != ErrInvalidCredentials {
		t.Errorf("Expected ErrInvalidCredentials, got %v", err)
	}
}

func TestLoginNonexistentUser(t *testing.T) {
	repo := NewMockRepository()
	service := NewService(repo, 24*time.Hour)

	_, _, err := service.Login(context.Background(), "noone@example.com", "password123")
	if err != ErrInvalidCredentials {
		t.Errorf("Expected ErrInvalidCredentials, got %v", err)
	}
}

func TestGetCurrentUser(t *testing.T) {
	repo := NewMockRepository()
	service := NewService(repo, 24*time.Hour)

	// Create user and session
	originalUser, session, err := service.Signup(context.Background(), "Test User", "test@example.com", "password123")
	if err != nil {
		t.Fatalf("Signup failed: %v", err)
	}

	// Get current user
	user, err := service.GetCurrentUser(context.Background(), session.ID)
	if err != nil {
		t.Fatalf("GetCurrentUser failed: %v", err)
	}

	if user.ID != originalUser.ID {
		t.Errorf("Expected user ID %s, got %s", originalUser.ID, user.ID)
	}
}

func TestGetCurrentUserExpiredSession(t *testing.T) {
	repo := NewMockRepository()
	service := NewService(repo, -1*time.Hour) // Negative duration = already expired

	// Create user with expired session
	_, session, err := service.Signup(context.Background(), "Test User", "test@example.com", "password123")
	if err != nil {
		t.Fatalf("Signup failed: %v", err)
	}

	// Try to get current user with expired session
	_, err = service.GetCurrentUser(context.Background(), session.ID)
	if err != ErrSessionExpired {
		t.Errorf("Expected ErrSessionExpired, got %v", err)
	}
}

func TestLogout(t *testing.T) {
	repo := NewMockRepository()
	service := NewService(repo, 24*time.Hour)

	// Create user and session
	_, session, err := service.Signup(context.Background(), "Test User", "test@example.com", "password123")
	if err != nil {
		t.Fatalf("Signup failed: %v", err)
	}

	// Logout
	if err := service.Logout(context.Background(), session.ID); err != nil {
		t.Fatalf("Logout failed: %v", err)
	}

	// Verify session is deleted
	_, err = service.GetCurrentUser(context.Background(), session.ID)
	if err != ErrSessionNotFound {
		t.Errorf("Expected ErrSessionNotFound after logout, got %v", err)
	}
}
