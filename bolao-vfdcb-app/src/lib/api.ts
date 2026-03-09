export const API_BASE_URL = 'http://localhost:8080';

export class ApiError extends Error {
    constructor(public status: number, message: string) {
        super(message);
        this.name = 'ApiError';
    }
}

export interface UserResponse {
    id: string;
    name: string;
    email: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface SignupRequest {
    name: string;
    email: string;
    password: string;
}

export async function fetchFromApi<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const path = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;

    try {
        const response = await fetch(`${API_BASE_URL}${path}`, {
            ...options,
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                ...options?.headers,
            },
        });

        if (!response.ok) {
            throw new ApiError(response.status, `API request failed: ${response.statusText}`);
        }

        // Handle empty responses
        if (response.status === 204) {
            return {} as T;
        }

        return response.json();
    } catch (error) {
        console.error('API Call failed:', error);
        throw error;
    }
}
