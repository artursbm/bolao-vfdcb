.PHONY: run test migrate clean

run:
	go run ./cmd/server

test:
	go test ./... -v

test-short:
	go test ./... -short -v

migrate-up:
	goose -dir migrations postgres "$(DATABASE_URL)" up

migrate-down:
	goose -dir migrations postgres "$(DATABASE_URL)" down

clean:
	rm -rf bin/

build:
	go build -o bin/server ./cmd/server
