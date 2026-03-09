package auth

import (
	"testing"

	"github.com/google/uuid"
)

func TestCookieSigning(t *testing.T) {
	secret := "test-secret-key-32-bytes-long!"
	sessionID := uuid.New().String()

	// Test sign and verify roundtrip
	signed := Sign(sessionID, secret)
	verified, err := Verify(signed, secret)
	if err != nil {
		t.Fatalf("Verify failed: %v", err)
	}

	if verified != sessionID {
		t.Errorf("Expected %s, got %s", sessionID, verified)
	}
}

func TestCookieVerifyInvalidSignature(t *testing.T) {
	secret := "test-secret-key-32-bytes-long!"
	wrongSecret := "wrong-secret-key-32-bytes-long!"
	sessionID := uuid.New().String()

	signed := Sign(sessionID, secret)
	_, err := Verify(signed, wrongSecret)
	if err != ErrInvalidSignature {
		t.Errorf("Expected ErrInvalidSignature, got %v", err)
	}
}

func TestCookieVerifyInvalidFormat(t *testing.T) {
	secret := "test-secret-key-32-bytes-long!"

	testCases := []string{
		"invalid",
		"no-dot-here",
		"only.one",
		"",
	}

	for _, tc := range testCases {
		_, err := Verify(tc, secret)
		if err != ErrInvalidFormat && err != ErrInvalidSignature {
			t.Errorf("For input %q, expected format or signature error, got %v", tc, err)
		}
	}
}
