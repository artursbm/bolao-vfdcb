# Stage 1: Build the SvelteKit app
FROM node:22-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code and build
COPY . .
RUN npm run build

# Stage 2: Create the minimal production image
FROM node:22-alpine

WORKDIR /app

# Copy the built app and package files
COPY --from=builder /app/build ./build
COPY --from=builder /app/package.json ./
COPY --from=builder /app/package-lock.json ./

# Install only production dependencies
RUN npm ci --omit=dev

EXPOSE 3000
# Start the Node server
ENTRYPOINT ["node", "build"]