import { fetchFromApi } from './api';

// ── Types ────────────────────────────────────────────────────────────────────

export interface Team {
    id: string;
    name: string;
    code: string;
    crest?: string;
}

export interface Match {
    id: string;
    home_team: Team;
    away_team: Team;
    match_time: string;
    home_score: number | null;
    away_score: number | null;
    status: 'TIMED' | 'IN_PLAY' | 'IN_PLAY' | 'PAUSED' | 'FINISHED';
    stage?: string;
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
        case 'TIMED': return 'Agendado';
        case 'IN_PLAY': return 'Em andamento';
        case 'IN_PLAY': return 'Em andamento';
        case 'PAUSED': return 'Interrompido';
        case 'FINISHED': return 'Finalizado';
    }
}

export function translateStage(stage?: string): string {
    if (!stage) return '';
    switch (stage) {
        case 'GROUP_STAGE': return 'Fase de grupos';
        case 'LAST_32': return '16 avos de final';
        case 'LAST_16': return 'Oitavas de final';
        case 'QUARTER_FINALS': return 'Quartas de final';
        case 'SEMI_FINALS': return 'Semifinais';
        case 'FINAL': return 'Final';
        case 'THIRD_PLACE': return 'Disputa do 3º lugar';
        default: return stage;
    }
}
