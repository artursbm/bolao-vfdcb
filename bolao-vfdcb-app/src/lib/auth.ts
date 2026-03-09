import { writable } from 'svelte/store';
import { fetchFromApi, type UserResponse, type LoginRequest, type SignupRequest } from './api';

// Create a writable store for the current user
export const user = writable<UserResponse | null>(null);
export const authLoading = writable<boolean>(true);

// Initialize authentication state from backend
export async function checkAuth() {
    authLoading.set(true);
    try {
        const userData = await fetchFromApi<UserResponse>('/api/auth/me');
        user.set(userData);
    } catch (error) {
        // Not authenticated
        user.set(null);
    } finally {
        authLoading.set(false);
    }
}

// Login
export async function login(data: LoginRequest) {
    const response = await fetchFromApi<UserResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(data)
    });
    user.set(response);
    return response;
}

// Signup
export async function signup(data: SignupRequest) {
    const response = await fetchFromApi<UserResponse>('/api/auth/signup', {
        method: 'POST',
        body: JSON.stringify(data)
    });
    user.set(response);
    return response;
}

// Logout
export async function logout() {
    try {
        await fetchFromApi('/api/auth/logout', { method: 'POST' });
    } finally {
        user.set(null);
    }
}
