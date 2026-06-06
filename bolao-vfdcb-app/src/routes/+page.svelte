<script lang="ts">
    import { onMount } from "svelte";
    import { user } from "$lib/auth";
    import { rankingStore } from "$lib/stores/ranking";
    import {
        fetchUpcomingMatches,
        formatMatchTime,
        statusLabel,
        type Match,
    } from "$lib/championship";

    const dateOnlyFormatter = new Intl.DateTimeFormat('pt-BR', {
        timeZone: 'America/Sao_Paulo',
        weekday: 'long',
        day: '2-digit',
        month: 'long',
        year: 'numeric',
    });

    const timeOnlyFormatter = new Intl.DateTimeFormat('pt-BR', {
        timeZone: 'America/Sao_Paulo',
        hour: '2-digit',
        minute: '2-digit',
    });

    function formatDateKey(isoDate: string): string {
        const d = new Date(isoDate);
        // key: YYYY-MM-DD in BRT so we can sort deterministically
        return new Intl.DateTimeFormat('en-CA', {
            timeZone: 'America/Sao_Paulo',
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
        }).format(d);
    }

    function formatDateLabel(isoDate: string): string {
        return dateOnlyFormatter.format(new Date(isoDate));
    }

    function formatTimeOnly(isoDate: string): string {
        return timeOnlyFormatter.format(new Date(isoDate));
    }

    let backendStatus = $state("Verificando...");
    let matches = $state<Match[]>([]);
    let loading = $state(true);

    onMount(async () => {
        try {
            await Promise.all([
                fetchUpcomingMatches().then((m) => (matches = m)),
                rankingStore.fetch(),
            ]);
            backendStatus = "Online";
        } catch (e) {
            console.error("Failed to load data", e);
            backendStatus = "Offline";
        } finally {
            loading = false;
        }
    });

    const topRankings = $derived($rankingStore.data.slice(0, 3));
    const userRanking = $derived.by(() => {
        if (!$user) return null;
        const index = $rankingStore.data.findIndex(
            (r) => r.user_id === $user.id,
        );
        if (index === -1) return null;
        return {
            ...$rankingStore.data[index],
            rank: index + 1,
        };
    });

    const isUserInTop3 = $derived(
        topRankings.some((r) => r.user_id === $user?.id),
    );

    // Group matches by calendar date (BRT timezone)
    const matchesByDate = $derived.by(() => {
        const map = new Map<string, { label: string; matches: Match[] }>();
        for (const match of matches) {
            const key = formatDateKey(match.match_time);
            if (!map.has(key)) {
                map.set(key, { label: formatDateLabel(match.match_time), matches: [] });
            }
            map.get(key)!.matches.push(match);
        }
        // Return entries sorted by date key (ascending)
        return [...map.entries()].sort(([a], [b]) => a.localeCompare(b));
    });

    function getBadgeClass(status: string) {
        if (status === "TIMED" || status === "SCHEDULED")
            return "badge-scheduled";
        if (status === "IN_PLAY" || status === "PAUSED") return "badge-live";
        if (status === "FINISHED") return "badge-finished";
        return "";
    }

    function getRankEmoji(index: number) {
        if (index === 0) return "🥇";
        if (index === 1) return "🥈";
        if (index === 2) return "🥉";
        return "";
    }
</script>

<div
    class="container home-container"
    style="padding-top: 4rem; padding-bottom: 4rem;"
>
    <header style="margin-bottom: 4rem; text-align: center;">
        <h1
            class="animate-fade-in"
            style="font-size: 3.5rem; background: linear-gradient(to right, #fff, #94a3b8); -webkit-background-clip: text; -webkit-text-fill-color: transparent;"
        >
            Bolão da Copa do Mundo 2026
        </h1>

        <div class="animate-fade-in">
            <span
                style="
        font-size: 0.875rem; 
        color: var(--color-text-muted); 
        background: var(--color-surface); 
        padding: 0.5rem 1rem; 
        border-radius: 9999px; 
        border: 1px solid var(--color-border);
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
      "
            >
                <span
                    style="
          width: 8px; 
          height: 8px; 
          border-radius: 50%; 
          background-color: {backendStatus === 'Online'
                        ? 'var(--color-accent)'
                        : 'var(--color-danger)'};
          display: inline-block;
        "
                ></span>
                Status da API: {backendStatus}
            </span>
        </div>
    </header>

    <div
        class="home-grid animate-fade-in"
        class:full-width={!$user}
        style="animation-delay: 0.1s;"
    >
        {#if $user}
            <!-- Left Column: Ranking (1/3) -->
            <aside class="ranking-sidebar">
                <div class="card" style="padding: 1.5rem;">
                    <div
                        style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;"
                    >
                        <h3 style="margin: 0; font-size: 1.25rem;">Ranking</h3>
                        <a
                            href="/ranking"
                            style="font-size: 0.875rem; color: var(--color-primary); text-decoration: none; font-weight: 500;"
                            >Ver tudo</a
                        >
                    </div>

                    {#if $rankingStore.loading && $rankingStore.data.length === 0}
                        <div
                            style="text-align: center; padding: 2rem; color: var(--color-text-muted);"
                        >
                            Carregando...
                        </div>
                    {:else if $rankingStore.data.length === 0}
                        <div
                            style="text-align: center; padding: 2rem; color: var(--color-text-muted); font-size: 0.875rem;"
                        >
                            Nenhuma pontuação ainda
                        </div>
                    {:else}
                        <div class="mini-ranking">
                            {#each topRankings as rank, i}
                                <div
                                    class="ranking-item {rank.user_id ===
                                    $user?.id
                                        ? 'active'
                                        : ''}"
                                >
                                    <div class="rank-num">
                                        {getRankEmoji(i)}
                                    </div>
                                    <div class="rank-name">
                                        {rank.user_name}
                                    </div>
                                    <div class="rank-pts">
                                        {rank.total_score} pts
                                    </div>
                                </div>
                            {/each}

                            {#if userRanking && !isUserInTop3}
                                <div class="ranking-divider"></div>
                                <div class="ranking-item active">
                                    <div
                                        class="rank-num"
                                        style="font-size: 0.75rem; color: var(--color-text-muted);"
                                    >
                                        {userRanking.rank}º
                                    </div>
                                    <div class="rank-name">
                                        {userRanking.user_name}
                                    </div>
                                    <div class="rank-pts">
                                        {userRanking.total_score} pts
                                    </div>
                                </div>
                            {/if}
                        </div>
                    {/if}
                </div>
            </aside>

            <div class="vertical-divider"></div>
        {/if}

        <!-- Right Column: Matches (2/3) -->
        <section class="matches-content">
            <div
                style="display: flex; justify-content: center; align-items: flex-end; margin-bottom: 2rem;"
            >
                <h2>Partidas</h2>
            </div>

            {#if loading}
                <div
                    style="text-align: center; padding: 3rem; color: var(--color-text-muted);"
                >
                    Carregando partidas...
                </div>
            {:else if matches.length === 0}
                <div
                    style="text-align: center; padding: 3rem; color: var(--color-text-muted); background: var(--color-surface); border-radius: var(--radius-lg); border: 1px solid var(--color-border);"
                >
                    Nenhuma partida agendada no momento.
                </div>
            {:else}
                {#each matchesByDate as [_key, group]}
                    <div class="date-group">
                        <div class="date-header">
                            <div class="date-header-line"></div>
                            <span class="date-header-label">{group.label}</span>
                            <div class="date-header-line"></div>
                        </div>

                        <div
                            style="display: grid; gap: 1.5rem; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));"
                        >
                            {#each group.matches as match}
                                <div class="card">
                                    <div
                                        style="display: flex; justify-content: center; margin-bottom: 1rem;"
                                    >
                                        <span
                                            class="badge {getBadgeClass(match.status)}"
                                        >
                                            {statusLabel(match.status)}
                                        </span>
                                    </div>

                                    <div
                                        style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;"
                                    >
                                        <div style="text-align: right; flex: 1;">
                                            <div
                                                style="font-weight: 700; font-size: 1.25rem; align-items: center; justify-content: flex-start;"
                                            >
                                                {#if match.home_team.crest}
                                                    <img
                                                        src={match.home_team.crest}
                                                        alt={match.home_team.name}
                                                        style="width: 1.25rem; height: 1.25rem; object-fit: contain;"
                                                    />
                                                {/if}
                                                <span>{match.home_team.code}</span>
                                            </div>
                                            <div
                                                style="color: var(--color-text-muted); font-size: 0.725rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"
                                            >
                                                {match.home_team.name}
                                            </div>
                                        </div>

                                        <div
                                            style="padding: 0 1rem; text-align: center;"
                                        >
                                            {#if match.status === "TIMED"}
                                                <div
                                                    style="
                                    color: var(--color-text-muted); 
                                    font-size: 0.875rem; 
                                    font-weight: 600; 
                                    padding: 0.25rem 0.75rem; 
                                    background: #0f172a; 
                                    border-radius: 1rem;
                                    "
                                                >
                                                    VS
                                                </div>
                                            {:else}
                                                <div
                                                    class="score-display"
                                                    style="background: #0f172a; padding: 0.5rem 1rem; border-radius: var(--radius-md);"
                                                >
                                                    {match.home_score} - {match.away_score}
                                                </div>
                                            {/if}
                                        </div>

                                        <div style="text-align: left; flex: 1;">
                                            <div
                                                style="font-weight: 700; font-size: 1.25rem; align-items: center; justify-content: flex-start;"
                                            >
                                                {#if match.away_team.crest}
                                                    <img
                                                        src={match.away_team.crest}
                                                        alt={match.away_team.name}
                                                        style="width: 1.25rem; height: 1.25rem; object-fit: contain;"
                                                    />
                                                {/if}
                                                <span>{match.away_team.code}</span>
                                            </div>
                                            <div
                                                style="color: var(--color-text-muted); font-size: 0.725rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"
                                            >
                                                {match.away_team.name}
                                            </div>
                                        </div>
                                    </div>

                                    <div
                                        style="text-align: center; margin-bottom: 1.5rem; padding: 1rem; background: rgba(255,255,255,0.03); border-radius: var(--radius-md);"
                                    >
                                        <div
                                            style="font-size: 0.875rem; color: var(--color-primary); font-weight: 600; letter-spacing: 0.5px;"
                                        >
                                            {formatTimeOnly(match.match_time)}
                                        </div>
                                    </div>
                                </div>
                            {/each}
                        </div>
                    </div>
                {/each}
            {/if}
        </section>
    </div>
</div>

<style>
    .home-container {
        max-width: 100%;
        padding-left: 1rem;
        padding-right: 1rem;
    }

    @media (min-width: 1280px) {
        .home-container {
            padding-left: 2rem;
            padding-right: 2rem;
        }
    }

    .home-grid {
        display: grid;
        grid-template-columns: 1fr;
        gap: 4rem;
        align-items: start;
    }

    .home-grid.full-width {
        grid-template-columns: 1fr !important;
    }

    .vertical-divider {
        display: none;
    }

    @media (min-width: 1024px) {
        .home-grid {
            grid-template-columns: 1fr auto 4fr; /* 1/5 ranking, 4/5 matches */
        }

        .vertical-divider {
            width: 1px;
            height: 100%;
            min-height: 400px;
            background: linear-gradient(
                to bottom,
                transparent,
                var(--color-border),
                transparent
            );
            display: block;
        }
    }

    .mini-ranking {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }

    .ranking-item {
        display: flex;
        align-items: center;
        padding: 0.75rem 1rem;
        border-radius: var(--radius-md);
        background: rgba(255, 255, 255, 0.02);
        transition: background 0.2s;
    }

    .ranking-item.active {
        background: rgba(var(--color-primary-rgb), 0.1);
        border: 1px solid rgba(var(--color-primary-rgb), 0.2);
    }

    .rank-num {
        width: 30px;
        font-weight: 700;
        font-size: 1.125rem;
    }

    .rank-name {
        flex: 1;
        font-weight: 500;
        font-size: 0.9375rem;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        padding: 0 0.5rem;
    }

    .rank-pts {
        font-weight: 700;
        color: var(--color-primary);
        font-size: 0.9375rem;
    }

    .ranking-divider {
        height: 1px;
        background: var(--color-border);
        margin: 0.5rem 0;
        border-style: dashed;
        background: transparent;
        border-bottom: 2px dashed var(--color-border);
    }

    /* ── Date grouping ─────────────────────────────────────────────────── */

    .date-group {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
        margin-bottom: 2.5rem;
    }

    .date-group:last-child {
        margin-bottom: 0;
    }

    .date-header {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .date-header-line {
        flex: 1;
        height: 1px;
        background: linear-gradient(
            to right,
            transparent,
            var(--color-border),
            transparent
        );
    }

    .date-header-label {
        white-space: nowrap;
        font-size: 0.8125rem;
        font-weight: 600;
        letter-spacing: 0.06em;
        text-transform: capitalize;
        color: var(--color-text-muted);
        background: var(--color-surface);
        padding: 0.3rem 0.9rem;
        border-radius: 9999px;
        border: 1px solid var(--color-border);
    }
</style>
