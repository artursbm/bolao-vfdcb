import { writable } from 'svelte/store';
import { fetchRanking, type UserRanking } from '../championship';

function createRankingStore() {
    const { subscribe, set, update } = writable<{
        data: UserRanking[];
        loading: boolean;
        error: string | null;
        lastFetched: number | null;
    }>({
        data: [],
        loading: false,
        error: null,
        lastFetched: null,
    });

    return {
        subscribe,
        fetch: async (force = false) => {
            update(s => ({ ...s, loading: true, error: null }));
            try {
                const data = await fetchRanking();
                set({
                    data,
                    loading: false,
                    error: null,
                    lastFetched: Date.now(),
                });
            } catch (err: any) {
                update(s => ({
                    ...s,
                    loading: false,
                    error: err.message || 'Failed to fetch ranking',
                }));
            }
        }
    };
}

export const rankingStore = createRankingStore();
