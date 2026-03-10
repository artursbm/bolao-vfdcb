import { fetchFromApi } from './api';

// ── Types ────────────────────────────────────────────────────────────────────

export interface Team {
    id: string;
    name: string;
    code: string;
}

export interface Match {
    id: string;
    home_team: Team;
    away_team: Team;
    match_time: string;
    home_score: number | null;
    away_score: number | null;
    status: 'SCHEDULED' | 'IN_PROGRESS' | 'FINISHED';
    created_at: string;
    updated_at: string;
}

export interface Guess {
    id: string;
    user_id: string;
    match_id: string;
    home_score: number;
    away_score: number;
    points: number | null;
    created_at: string;
    updated_at: string;
}

export interface GuessWithMatch extends Guess {
    match: Match;
}

export interface UserRanking {
    user_id: string;
    user_name: string;
    total_score: number;
}

// ── API Functions ────────────────────────────────────────────────────────────

export async function fetchUpcomingMatches(): Promise<Match[]> {
    return fetchFromApi<Match[]>('/api/matches');
}

export async function fetchUserGuesses(): Promise<GuessWithMatch[]> {
    return fetchFromApi<GuessWithMatch[]>('/api/guesses');
}

export async function fetchRanking(): Promise<UserRanking[]> {
    return fetchFromApi<UserRanking[]>('/api/ranking');
}

export async function submitGuess(matchId: string, homeScore: number, awayScore: number): Promise<Guess> {
    return fetchFromApi<Guess>('/api/guesses', {
        method: 'POST',
        body: JSON.stringify({
            match_id: matchId,
            home_score: homeScore,
            away_score: awayScore,
        }),
    });
}

// ── Helpers ──────────────────────────────────────────────────────────────────

const brtFormatter = new Intl.DateTimeFormat('pt-BR', {
    timeZone: 'America/Sao_Paulo',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
});

export function formatMatchTime(isoDate: string): string {
    return brtFormatter.format(new Date(isoDate));
}

export function statusLabel(status: Match['status']): string {
    switch (status) {
        case 'SCHEDULED': return 'Previsto';
        case 'IN_PROGRESS': return 'Em andamento';
        case 'FINISHED': return 'Finalizado';
    }
}
