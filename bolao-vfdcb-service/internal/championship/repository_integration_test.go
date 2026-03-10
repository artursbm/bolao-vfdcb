package championship

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

func TestIntegrationRepositoryGetRanking(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test")
	}

	repo := NewRepository(testPool)
	ctx := context.Background()

	// 1. Setup Data - Users
	user1ID := uuid.New()
	user2ID := uuid.New()
	user3ID := uuid.New()
	_, err := testPool.Exec(ctx, "INSERT INTO users (id, name, email, password) VALUES ($1, 'Alice', 'alice@test.com', 'pass')", user1ID)
	if err != nil {
		t.Fatalf("Failed to insert user Alice: %v", err)
	}
	_, err = testPool.Exec(ctx, "INSERT INTO users (id, name, email, password) VALUES ($1, 'Bob', 'bob@test.com', 'pass')", user2ID)
	if err != nil {
		t.Fatalf("Failed to insert user Bob: %v", err)
	}
	_, err = testPool.Exec(ctx, "INSERT INTO users (id, name, email, password) VALUES ($1, 'Charlie', 'charlie@test.com', 'pass')", user3ID)
	if err != nil {
		t.Fatalf("Failed to insert user Charlie: %v", err)
	}

	// 2. Setup Data - Teams & Matches (use distinct names from the seed)
	team1ID := uuid.New()
	team2ID := uuid.New()
	_, err = testPool.Exec(ctx, "INSERT INTO teams (id, name, code) VALUES ($1, 'Test Team A', 'TTA')", team1ID)
	if err != nil {
		t.Fatalf("Failed to insert TTA: %v", err)
	}
	_, err = testPool.Exec(ctx, "INSERT INTO teams (id, name, code) VALUES ($1, 'Test Team B', 'TTB')", team2ID)
	if err != nil {
		t.Fatalf("Failed to insert TTB: %v", err)
	}

	match1ID := uuid.New()
	match2ID := uuid.New()
	now := time.Now()
	_, err = testPool.Exec(ctx, "INSERT INTO matches (id, home_team_id, away_team_id, match_time, status) VALUES ($1, $2, $3, $4, 'SCHEDULED')", match1ID, team1ID, team2ID, now.Add(time.Hour))
	if err != nil {
		t.Fatalf("Failed to insert match 1: %v", err)
	}
	_, err = testPool.Exec(ctx, "INSERT INTO matches (id, home_team_id, away_team_id, match_time, status) VALUES ($1, $2, $3, $4, 'SCHEDULED')", match2ID, team2ID, team1ID, now.Add(2*time.Hour))
	if err != nil {
		t.Fatalf("Failed to insert match 2: %v", err)
	}

	// 3. Setup Data - Guesses (with computed points)
	// Alice gets 4 points in match 1, 3 points in match 2 -> Total = 7
	// Bob gets 0 points in match 1, 2 points in match 2   -> Total = 2
	// Charlie has NO guesses -> Total = 0
	_, err = testPool.Exec(ctx, "INSERT INTO guesses (user_id, match_id, home_score, away_score, points) VALUES ($1, $2, 2, 1, 4)", user1ID, match1ID)
	if err != nil {
		t.Fatalf("Failed to insert guess: %v", err)
	}
	_, err = testPool.Exec(ctx, "INSERT INTO guesses (user_id, match_id, home_score, away_score, points) VALUES ($1, $2, 1, 0, 3)", user1ID, match2ID)
	if err != nil {
		t.Fatalf("Failed to insert guess: %v", err)
	}

	_, err = testPool.Exec(ctx, "INSERT INTO guesses (user_id, match_id, home_score, away_score, points) VALUES ($1, $2, 0, 3, 0)", user2ID, match1ID)
	if err != nil {
		t.Fatalf("Failed to insert guess: %v", err)
	}
	_, err = testPool.Exec(ctx, "INSERT INTO guesses (user_id, match_id, home_score, away_score, points) VALUES ($1, $2, 2, 0, 2)", user2ID, match2ID)
	if err != nil {
		t.Fatalf("Failed to insert guess: %v", err)
	}

	// 4. Test GetRanking
	ranking, err := repo.GetRanking(ctx)
	if err != nil {
		t.Fatalf("GetRanking failed: %v", err)
	}

	// Expected ranking: Alice (7), Bob (2), Charlie (0).
	// If the database has existing seed data due to migrations, it might pick up more users. But our inserted users should be identifiable.
	// We'll verify relative ordering of Alice, Bob, Charlie.

	var aliceRank, bobRank, charlieRank *UserRanking
	for i := range ranking {
		if ranking[i].UserID == user1ID {
			aliceRank = &ranking[i]
		}
		if ranking[i].UserID == user2ID {
			bobRank = &ranking[i]
		}
		if ranking[i].UserID == user3ID {
			charlieRank = &ranking[i]
		}
	}

	if aliceRank == nil || bobRank == nil || charlieRank == nil {
		t.Fatalf("Expected Alice, Bob, and Charlie in ranking. Got: %+v", ranking)
	}

	if aliceRank.TotalScore != 7 {
		t.Errorf("Expected Alice to have 7 points, got %d", aliceRank.TotalScore)
	}
	if bobRank.TotalScore != 2 {
		t.Errorf("Expected Bob to have 2 points, got %d", bobRank.TotalScore)
	}
	if charlieRank.TotalScore != 0 {
		t.Errorf("Expected Charlie to have 0 points, got %d", charlieRank.TotalScore)
	}

	// Find their indices to map sorting correctness
	aliceIndex := -1
	bobIndex := -1
	charlieIndex := -1

	for i, r := range ranking {
		if r.UserID == user1ID {
			aliceIndex = i
		}
		if r.UserID == user2ID {
			bobIndex = i
		}
		if r.UserID == user3ID {
			charlieIndex = i
		}
	}

	if aliceIndex > bobIndex {
		t.Errorf("Expected Alice to be ranked higher than Bob")
	}
	if bobIndex > charlieIndex {
		t.Errorf("Expected Bob to be ranked higher than Charlie")
	}
}
