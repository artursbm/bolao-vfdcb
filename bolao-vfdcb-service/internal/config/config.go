package config

import (
	"time"

	"github.com/caarlos0/env/v11"
)

type Config struct {
	DatabaseURL     string        `env:"DATABASE_URL,required"`
	CookieHashKey   string        `env:"COOKIE_HASH_KEY,required"`
	ServerPort      string        `env:"SERVER_PORT" envDefault:"8080"`
	SessionDuration time.Duration `env:"SESSION_DURATION" envDefault:"720h"` // 30 days
	AllowedOrigins  []string      `env:"ALLOWED_ORIGINS" envDefault:"http://localhost:5173"`

	// Scoring points (configurable per sweepstake rules)
	ScoreExact      int `env:"SCORE_EXACT"       envDefault:"4"`
	ScoreWinnerDiff int `env:"SCORE_WINNER_DIFF" envDefault:"3"`
	ScoreWinner     int `env:"SCORE_WINNER"      envDefault:"2"`
	ScoreDraw       int `env:"SCORE_DRAW"        envDefault:"1"`
}

func Load() (*Config, error) {
	cfg := &Config{}
	if err := env.Parse(cfg); err != nil {
		return nil, err
	}
	return cfg, nil
}
