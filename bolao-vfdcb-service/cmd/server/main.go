package main

import (
	"context"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/artursbm/bolao-vfdcb-service/internal/auth"
	"github.com/artursbm/bolao-vfdcb-service/internal/championship"
	"github.com/artursbm/bolao-vfdcb-service/internal/config"
	"github.com/artursbm/bolao-vfdcb-service/internal/database"
	"github.com/artursbm/bolao-vfdcb-service/internal/migrations"
	"github.com/artursbm/bolao-vfdcb-service/internal/server"
	_ "github.com/jackc/pgx/v5/stdlib"
	"github.com/joho/godotenv"
	"github.com/pressly/goose/v3"
)

func main() {
	// Set up logger
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))

	// Load .env file if present
	if err := godotenv.Load(); err != nil {
		logger.Info("no .env file found, relying on system environment variables")
	}

	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		logger.Error("failed to load config", "error", err)
		os.Exit(1)
	}

	ctx := context.Background()

	// Connect to database
	pool, err := database.NewPool(ctx, cfg.DatabaseURL)
	if err != nil {
		logger.Error("failed to connect to database", "error", err)
		os.Exit(1)
	}
	defer pool.Close()

	// Run migrations
	goose.SetBaseFS(migrations.FS)
	if err := goose.SetDialect("postgres"); err != nil {
		logger.Error("failed to set goose dialect", "error", err)
		os.Exit(1)
	}

	db, err := goose.OpenDBWithDriver("pgx", cfg.DatabaseURL)
	if err != nil {
		logger.Error("failed to open DB for migrations", "error", err)
		os.Exit(1)
	}
	defer db.Close()

	if err := goose.Up(db, "."); err != nil {
		logger.Error("failed to run migrations", "error", err)
		os.Exit(1)
	}

	logger.Info("migrations completed successfully")

	// Initialize auth module
	authRepo := auth.NewRepository(pool)
	authService := auth.NewService(authRepo, cfg.SessionDuration)
	authHandler := auth.NewHandler(authService, cfg.CookieHashKey, logger)
	authMiddleware := auth.NewMiddleware(authService, cfg.CookieHashKey, logger)

	// Initialize championship module
	scoring := championship.ScoringConfig{
		Exact:      cfg.ScoreExact,
		WinnerDiff: cfg.ScoreWinnerDiff,
		Winner:     cfg.ScoreWinner,
		Draw:       cfg.ScoreDraw,
	}
	champRepo := championship.NewRepository(pool)
	champService := championship.NewService(champRepo, scoring)
	champHandler := championship.NewHandler(champService, logger)

	// Set up router
	handler := server.NewRouter(authHandler, authMiddleware, champHandler, logger)

	// Create HTTP server
	srv := &http.Server{
		Addr:         ":" + cfg.ServerPort,
		Handler:      handler,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Start server in goroutine
	go func() {
		logger.Info("starting server", "port", cfg.ServerPort)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("server failed", "error", err)
			os.Exit(1)
		}
	}()

	// Wait for interrupt signal for graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("shutting down server...")

	// Graceful shutdown with 30s timeout
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := srv.Shutdown(shutdownCtx); err != nil {
		logger.Error("server forced to shutdown", "error", err)
	}

	logger.Info("server exited")
}
