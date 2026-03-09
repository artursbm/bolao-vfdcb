package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"strings"
)

var (
	ErrInvalidSignature = errors.New("invalid signature")
	ErrInvalidFormat    = errors.New("invalid cookie format")
)

// Sign creates an HMAC-SHA256 signed cookie value
// Format: base64(data).base64(hmac-sha256(data, secret))
func Sign(data, secret string) string {
	dataB64 := base64.URLEncoding.EncodeToString([]byte(data))
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(data))
	signature := base64.URLEncoding.EncodeToString(mac.Sum(nil))
	return dataB64 + "." + signature
}

// Verify validates an HMAC-SHA256 signed cookie value and returns the original data
func Verify(signedValue, secret string) (string, error) {
	parts := strings.Split(signedValue, ".")
	if len(parts) != 2 {
		return "", ErrInvalidFormat
	}

	dataB64, signatureB64 := parts[0], parts[1]

	// Decode the data
	data, err := base64.URLEncoding.DecodeString(dataB64)
	if err != nil {
		return "", ErrInvalidFormat
	}

	// Decode the signature
	signature, err := base64.URLEncoding.DecodeString(signatureB64)
	if err != nil {
		return "", ErrInvalidFormat
	}

	// Compute expected signature
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(data)
	expectedSignature := mac.Sum(nil)

	// Constant-time comparison
	if !hmac.Equal(signature, expectedSignature) {
		return "", ErrInvalidSignature
	}

	return string(data), nil
}
