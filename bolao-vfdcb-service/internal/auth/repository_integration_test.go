package auth

import (
	"context"
	"database/sql"
	"flag"
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/artursbm/bolao-vfdcb-service/internal/migrations"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	_ "github.com/jackc/pgx/v5/stdlib"
	"github.com/pressly/goose/v3"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"
)

var testPool *pgxpool.Pool

func TestMain(m *testing.M) {
	// Parse flags to check for -short
	flag.Parse()

	// For unit tests with -short flag, skip container setup
	if testing.Short() {
		os.Exit(m.Run())
	}

	ctx := context.Background()

	// Start Postgres container
	pgContainer, err := postgres.Run(ctx,
		"postgres:16-alpine",
		postgres.WithDatabase("bolao_test"),
		postgres.WithUsername("test"),
		postgres.WithPassword("test"),
		testcontainers.WithWaitStrategy(
			wait.ForLog("database system is ready to accept connections").
				WithOccurrence(2).
				WithStartupTimeout(30*time.Second)),
	)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to start postgres container: %v\n", err)
		os.Exit(1)
	}
	defer func() {
		if err := testcontainers.TerminateContainer(pgContainer); err != nil {
			fmt.Fprintf(os.Stderr, "Failed to terminate container: %v\n", err)
		}
	}()

	// Get connection string
	connStr, err := pgContainer.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to get connection string: %v\n", err)
		os.Exit(1)
	}

	// Run migrations
	goose.SetBaseFS(migrations.FS)
	if err := goose.SetDialect("postgres"); err != nil {
		fmt.Fprintf(os.Stderr, "Failed to set goose dialect: %v\n", err)
		os.Exit(1)
	}

	db, err := sql.Open("pgx", connStr)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to open DB: %v\n", err)
		os.Exit(1)
	}
	defer db.Close()

	if err := goose.Up(db, "."); err != nil {
		fmt.Fprintf(os.Stderr, "Failed to run migrations: %v\n", err)
		os.Exit(1)
	}

	// Create connection pool
	testPool, err = pgxpool.New(ctx, connStr)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to create pool: %v\n", err)
		os.Exit(1)
	}
	defer testPool.Close()

	// Run tests
	code := m.Run()
	os.Exit(code)
}

func TestIntegrationRepositoryCreateUser(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test")
	}

	repo := NewRepository(testPool)
	ctx := context.Background()

	user, err := repo.CreateUser(ctx, "Integration Test", "integration@test.com", "hashedpassword")
	if err != nil {
		t.Fatalf("CreateUser failed: %v", err)
	}

	if user.Name != "Integration Test" {
		t.Errorf("Expected name 'Integration Test', got %s", user.Name)
	}
	if user.Email != "integration@test.com" {
		t.Errorf("Expected email 'integration@test.com', got %s", user.Email)
	}
	if user.ID == uuid.Nil {
		t.Error("Expected valid UUID, got nil")
	}
}

func TestIntegrationRepositoryGetUserByEmail(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test")
	}

	repo := NewRepository(testPool)
	ctx := context.Background()

	// Create user
	createdUser, err := repo.CreateUser(ctx, "Test User", "getbyemail@test.com", "hashedpass")
	if err != nil {
		t.Fatalf("CreateUser failed: %v", err)
	}

	// Get user by email
	user, err := repo.GetUserByEmail(ctx, "getbyemail@test.com")
	if err != nil {
		t.Fatalf("GetUserByEmail failed: %v", err)
	}

	if user.ID != createdUser.ID {
		t.Errorf("Expected user ID %s, got %s", createdUser.ID, user.ID)
	}
}

func TestIntegrationRepositoryGetUserByID(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test")
	}

	repo := NewRepository(testPool)
	ctx := context.Background()

	// Create user
	createdUser, err := repo.CreateUser(ctx, "Test User", "getbyid@test.com", "hashedpass")
	if err != nil {
		t.Fatalf("CreateUser failed: %v", err)
	}

	// Get user by ID
	user, err := repo.GetUserByID(ctx, createdUser.ID)
	if err != nil {
		t.Fatalf("GetUserByID failed: %v", err)
	}

	if user.Email != "getbyid@test.com" {
		t.Errorf("Expected email 'getbyid@test.com', got %s", user.Email)
	}
}

func TestIntegrationRepositorySession(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test")
	}

	repo := NewRepository(testPool)
	ctx := context.Background()

	// Create user
	user, err := repo.CreateUser(ctx, "Session Test", "session@test.com", "hashedpass")
	if err != nil {
		t.Fatalf("CreateUser failed: %v", err)
	}

	// Create session
	expiresAt := time.Now().Add(24 * time.Hour)
	session, err := repo.CreateSession(ctx, user.ID, expiresAt)
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}

	if session.UserID != user.ID {
		t.Errorf("Expected user ID %s, got %s", user.ID, session.UserID)
	}

	// Get session
	retrievedSession, err := repo.GetSessionByID(ctx, session.ID)
	if err != nil {
		t.Fatalf("GetSessionByID failed: %v", err)
	}

	if retrievedSession.ID != session.ID {
		t.Errorf("Expected session ID %s, got %s", session.ID, retrievedSession.ID)
	}

	// Delete session
	if err := repo.DeleteSession(ctx, session.ID); err != nil {
		t.Fatalf("DeleteSession failed: %v", err)
	}

	// Verify deletion
	_, err = repo.GetSessionByID(ctx, session.ID)
	if err != ErrSessionNotFound {
		t.Errorf("Expected ErrSessionNotFound, got %v", err)
	}
}
