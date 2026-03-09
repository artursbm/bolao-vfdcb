<script lang="ts">
    import { onMount } from "svelte";
    import {
        fetchUpcomingMatches,
        formatMatchTime,
        statusLabel,
        type Match,
    } from "$lib/championship";

    let backendStatus = $state("Verificando...");
    let matches = $state<Match[]>([]);
    let loading = $state(true);

    onMount(async () => {
        try {
            matches = await fetchUpcomingMatches();
            backendStatus = "Online";
        } catch (e) {
            console.error("Failed to load matches", e);
            backendStatus = "Offline";
        } finally {
            loading = false;
        }
    });

    function getBadgeClass(status: string) {
        if (status === "SCHEDULED") return "badge-scheduled";
        if (status === "IN_PROGRESS") return "badge-live";
        if (status === "FINISHED") return "badge-finished";
        return "";
    }
</script>

<div class="container" style="padding-top: 4rem; padding-bottom: 4rem;">
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

    <section class="animate-fade-in" style="animation-delay: 0.1s;">
        <div
            style="display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 2rem;"
        >
            <h2>Próximas Partidas</h2>
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
            <div
                style="display: grid; gap: 1.5rem; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));"
            >
                {#each matches as match}
                    <div class="card">
                        <div
                            style="display: flex; justify-content: center; margin-bottom: 1rem;"
                        >
                            <span class="badge {getBadgeClass(match.status)}">
                                {statusLabel(match.status)}
                            </span>
                        </div>

                        <div
                            style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;"
                        >
                            <div style="text-align: right; flex: 1;">
                                <div
                                    style="font-weight: 700; font-size: 1.25rem;"
                                >
                                    {match.home_team.name}
                                </div>
                                <div
                                    style="color: var(--color-text-muted); font-size: 0.875rem;"
                                >
                                    {match.home_team.code}
                                </div>
                            </div>

                            <div style="padding: 0 1rem; text-align: center;">
                                {#if match.status === "SCHEDULED"}
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
                                    style="font-weight: 700; font-size: 1.25rem;"
                                >
                                    {match.away_team.name}
                                </div>
                                <div
                                    style="color: var(--color-text-muted); font-size: 0.875rem;"
                                >
                                    {match.away_team.code}
                                </div>
                            </div>
                        </div>

                        <div
                            style="text-align: center; margin-bottom: 1.5rem; padding: 1rem; background: rgba(255,255,255,0.03); border-radius: var(--radius-md);"
                        >
                            <div
                                style="font-size: 0.875rem; color: var(--color-primary); font-weight: 600; letter-spacing: 0.5px;"
                            >
                                {formatMatchTime(match.match_time)}
                            </div>
                        </div>
                    </div>
                {/each}
            </div>
        {/if}
    </section>
</div>
