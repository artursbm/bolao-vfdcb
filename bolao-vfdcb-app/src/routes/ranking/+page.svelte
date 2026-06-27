<script lang="ts">
    import { onMount } from "svelte";
    import { rankingStore } from "$lib/stores/ranking";
    import { user } from "$lib/auth";

    onMount(() => {
        rankingStore.fetch();
    });

    function getRankColor(index: number) {
        if (index === 0) return "#fbbf24"; // Gold
        if (index === 1) return "#94a3b8"; // Silver
        if (index === 2) return "#b45309"; // Bronze
        return "transparent";
    }

    function getRowClass(userId: string) {
        return userId === $user?.id ? "current-user-row" : "";
    }
</script>

<div class="container animate-fade-in" style="padding-top: 4rem; padding-bottom: 4rem;">
    <header style="margin-bottom: 3rem;">
        <h1 style="font-size: 2.5rem; margin-bottom: 0.5rem;">Ranking Geral</h1>
        <p style="color: var(--color-text-muted);">Confira a pontuação de todos os participantes</p>
    </header>

    {#if $rankingStore.loading && $rankingStore.data.length === 0}
        <div class="loading-container">
            <div class="spinner"></div>
            <p>Carregando ranking...</p>
        </div>
    {:else if $rankingStore.error}
        <div class="error-container">
            <p>{$rankingStore.error}</p>
            <button class="btn-primary" onclick={() => rankingStore.fetch(true)}>Tentar novamente</button>
        </div>
    {:else}
        <div class="table-container card">
            <table class="ranking-table">
                <thead>
                    <tr>
                        <th style="width: 80px;">Posição</th>
                        <th>Participante</th>
                        <th style="width: 120px; text-align: right;">Pontos</th>
                    </tr>
                </thead>
                <tbody>
                    {#each $rankingStore.data as ur, i}
                        <tr class={getRowClass(ur.user_id)}>
                            <td>
                                <div class="rank-badge" style="background-color: {getRankColor(i)}; border: {i > 2 ? '1px solid var(--color-border)' : 'none'}">
                                    {i + 1}º
                                </div>
                            </td>
                            <td>
                                <div style="display: flex; align-items: center; gap: 0.75rem;">
                                    <span class="user-name">{ur.user_name}</span>
                                    {#if ur.user_id === $user?.id}
                                        <span class="badge-user">Você</span>
                                    {/if}
                                </div>
                            </td>
                            <td style="text-align: right; font-weight: 700; font-size: 1.125rem; color: var(--color-primary);">
                                {ur.total_score}
                            </td>
                        </tr>
                    {/each}
                </tbody>
            </table>
        </div>
    {/if}
</div>

<style>
    .loading-container, .error-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 5rem;
        background: var(--color-surface);
        border-radius: var(--radius-lg);
        border: 1px solid var(--color-border);
    }

    .table-container {
        padding: 0;
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
    }

    .ranking-table {
        width: 100%;
        border-collapse: collapse;
        text-align: left;
    }

    .ranking-table th {
        padding: 1.25rem 1.5rem;
        font-weight: 600;
        color: var(--color-text-muted);
        font-size: 0.875rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        border-bottom: 1px solid var(--color-border);
        background: rgba(255, 255, 255, 0.02);
    }

    .ranking-table td {
        padding: 1.25rem 1.5rem;
        border-bottom: 1px solid var(--color-border);
    }

    .ranking-table tr:last-child td {
        border-bottom: none;
    }

    .ranking-table tr:hover {
        background: rgba(255, 255, 255, 0.02);
    }

    .current-user-row {
        background: rgba(var(--color-primary-rgb), 0.1) !important;
    }

    .rank-badge {
        width: 36px;
        height: 36px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        font-weight: 700;
        font-size: 0.875rem;
        color: #fff;
    }

    .user-name {
        font-weight: 600;
        font-size: 1rem;
    }

    .badge-user {
        font-size: 0.625rem;
        text-transform: uppercase;
        background: var(--color-primary);
        color: white;
        padding: 0.125rem 0.5rem;
        border-radius: 9999px;
        font-weight: 700;
    }

    .spinner {
        width: 40px;
        height: 40px;
        border: 3px solid rgba(255, 255, 255, 0.1);
        border-top-color: var(--color-primary);
        border-radius: 50%;
        animation: spin 1s linear infinite;
        margin-bottom: 1rem;
    }

    @keyframes spin {
        to { transform: rotate(360deg); }
    }

    @media (max-width: 640px) {
        .container {
            padding: 2rem 0.5rem !important;
        }

        .card {
            padding: 1rem;
        }

        header h1 {
            font-size: 2rem !important;
        }

        .ranking-table {
            min-width: 320px;
        }

        .ranking-table th {
            padding: 0.75rem 0.5rem;
            font-size: 0.75rem;
        }

        .ranking-table td {
            padding: 0.75rem 0.5rem;
        }

        .rank-badge {
            width: 28px;
            height: 28px;
            font-size: 0.75rem;
        }

        .user-name {
            font-size: 0.85rem;
        }

        /* Adjust column widths on mobile to reduce separation */
        .ranking-table th:first-child,
        .ranking-table td:first-child {
            width: 50px !important;
        }
        
        .ranking-table th:last-child,
        .ranking-table td:last-child {
            width: 60px !important;
            text-align: right;
        }
    }
</style>
