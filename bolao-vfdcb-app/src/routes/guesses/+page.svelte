<script lang="ts">
    import { onMount } from "svelte";
    import { goto } from "$app/navigation";
    import { user, authLoading } from "$lib/auth";
    import {
        fetchUserGuesses,
        submitGuess,
        statusLabel,
        translateStage,
        type GuessWithMatch,
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
        return new Intl.DateTimeFormat('en-CA', {
            timeZone: 'America/Sao_Paulo',
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
        }).format(new Date(isoDate));
    }

    function formatDateLabel(isoDate: string): string {
        return dateOnlyFormatter.format(new Date(isoDate));
    }

    function formatTimeOnly(isoDate: string): string {
        return timeOnlyFormatter.format(new Date(isoDate));
    }

    let loading = $state(true);
    let guesses = $state<GuessWithMatch[]>([]);

    // Group guesses by calendar date (BRT timezone)
    const guessesByDate = $derived.by(() => {
        const map = new Map<string, { label: string; items: GuessWithMatch[] }>();
        for (const item of guesses) {
            const key = formatDateKey(item.match.match_time);
            if (!map.has(key)) {
                map.set(key, { label: formatDateLabel(item.match.match_time), items: [] });
            }
            map.get(key)!.items.push(item);
        }
        return [...map.entries()].sort(([a], [b]) => a.localeCompare(b));
    });

    // UI state
    let editingMatchId = $state<string | null>(null);
    let editHomeScore = $state<string>("");
    let editAwayScore = $state<string>("");
    let submitError = $state<string>("");
    let submitSuccess = $state<string>("");
    let isSubmitting = $state(false);

    $effect(() => {
        if (!$authLoading && !$user) {
            goto("/login");
        }
    });

    onMount(async () => {
        try {
            guesses = await fetchUserGuesses();
        } catch (e: any) {
            if (e.status === 401) {
                goto("/login");
                return;
            }
            console.error("Failed to load guesses", e);
        } finally {
            loading = false;
        }
    });

    function getBadgeClass(status: string) {
        if (status === "TIMED" || status === "SCHEDULED") return "badge-scheduled";
        if (status === "IN_PLAY" || status === "PAUSED") return "badge-live";
        if (status === "FINISHED") return "badge-finished";
        return "";
    }

    function startEdit(
        matchId: string,
        currentHome: number,
        currentAway: number,
    ) {
        editingMatchId = matchId;
        // if the guess is real (has an ID), populate form. If it's a dummy guess (id is empty), leave fields empty.
        editHomeScore = currentHome >= 0 ? currentHome.toString() : "";
        editAwayScore = currentAway >= 0 ? currentAway.toString() : "";
        submitError = "";
        submitSuccess = "";
    }

    function cancelEdit() {
        editingMatchId = null;
        submitError = "";
    }

    async function handleSubmitGuess(matchId: string) {
        const hScore = parseInt(editHomeScore, 10);
        const aScore = parseInt(editAwayScore, 10);

        if (isNaN(hScore) || isNaN(aScore) || hScore < 0 || aScore < 0) {
            submitError = "Por favor, insira placares válidos.";
            return;
        }

        isSubmitting = true;
        submitError = "";
        submitSuccess = "";

        try {
            await submitGuess(matchId, hScore, aScore);

            // Refresh data
            guesses = await fetchUserGuesses();

            submitSuccess = "Palpite salvo com sucesso!";
            editingMatchId = null;

            // Clear toast after 3s
            setTimeout(() => {
                submitSuccess = "";
            }, 3000);
        } catch (e: any) {
            submitError =
                e.message || "Falha ao salvar palpite. O jogo já começou?";
        } finally {
            isSubmitting = false;
        }
    }
</script>

<div class="container" style="padding-top: 3rem; padding-bottom: 4rem;">
    <header style="margin-bottom: 3rem;">
        <h1 class="animate-fade-in">Meus Palpites</h1>
        <p class="animate-fade-in" style="font-size: 1.125rem;">
            Seus palpites para todos os jogos do mundial. Você pode alterar os
            placares até o horário de início de cada partida.
        </p>
    </header>

    <!-- Global Toasts -->
    {#if submitSuccess}
        <div class="toast toast-success">
            {submitSuccess}
        </div>
    {/if}

    {#if loading}
        <div
            style="text-align: center; padding: 4rem; color: var(--color-text-muted);"
        >
            Carregando seus palpites...
        </div>
    {:else if guesses.length === 0}
        <div
            style="text-align: center; padding: 4rem; background: var(--color-surface); border-radius: var(--radius-lg); border: 1px solid var(--color-border);"
        >
            Nenhuma partida encontrada.
        </div>
    {:else}
        <div style="display: flex; flex-direction: column; gap: 2.5rem;">
            {#each guessesByDate as [_key, group]}
                <div class="date-group">
                    <div class="date-header">
                        <div class="date-header-line"></div>
                        <span class="date-header-label">{group.label}</span>
                        <div class="date-header-line"></div>
                    </div>

                    <div style="display: flex; flex-direction: column; gap: 1rem;">
                        {#each group.items as item}
                            {@const isEditable = item.match.status === "TIMED"}
                            {@const hasRealGuess =
                                item.id &&
                                item.id !== "00000000-0000-0000-0000-000000000000"}

                            <div class="card" style="padding: 1.5rem;">
                                <!-- Header: Status and Time -->
                                <div
                                    style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; border-bottom: 1px solid var(--color-border); padding-bottom: 1rem;"
                                >
                                    <span class="badge {getBadgeClass(item.match.status)}">
                                        {statusLabel(item.match.status)}
                                    </span>
                                    <div
                                        style="font-size: 0.875rem; color: var(--color-text-muted); text-align: right;"
                                    >
                                        <div>{formatTimeOnly(item.match.match_time)}</div>
                                        {#if item.match.stage}
                                            <div style="font-size: 0.725rem; margin-top: 0.25rem;">
                                                {translateStage(item.match.stage)}
                                            </div>
                                        {/if}
                                    </div>
                                </div>

                                <!-- Layout for teams and score/form -->
                                <div
                                    style="display: flex; justify-content: space-between; align-items: center;"
                                >
                                    <!-- Home Team -->
                                    <div
                                        style="flex: 1; text-align: right; padding-right: 1.5rem; min-width: 0;"
                                    >
                                        <div style="font-weight: 700; font-size: 1.25rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                            {item.match.home_team?.name ?? "A definir"}
                                        </div>
                                        <div
                                            style="color: var(--color-text-muted); font-size: 0.875rem; display: flex; align-items: center; justify-content: flex-end; gap: 0.375rem;"
                                        >
                                            {#if item.match.home_team?.crest}
                                                <img
                                                    src={item.match.home_team.crest}
                                                    alt={item.match.home_team?.name ?? "A definir"}
                                                    style="width: 1.25rem; height: 1.25rem; object-fit: contain;"
                                                />
                                            {/if}
                                            <span>{item.match.home_team?.code ?? "TBD"}</span>
                                        </div>
                                    </div>

                                    <!-- Center: Form OR Display Score -->
                                    <div
                                        style="flex: 0 0 auto; min-width: 140px; text-align: center;"
                                    >
                                        {#if editingMatchId === item.match.id}
                                            <!-- EDIT MODE -->
                                            <div
                                                style="display: flex; align-items: center; justify-content: center; gap: 0.5rem;"
                                            >
                                                <input
                                                    type="number"
                                                    min="0"
                                                    max="99"
                                                    class="score-input"
                                                    bind:value={editHomeScore}
                                                    disabled={isSubmitting}
                                                />
                                                <span
                                                    style="color: var(--color-text-muted); font-weight: bold;"
                                                    >x</span
                                                >
                                                <input
                                                    type="number"
                                                    min="0"
                                                    max="99"
                                                    class="score-input"
                                                    bind:value={editAwayScore}
                                                    disabled={isSubmitting}
                                                />
                                            </div>
                                        {:else}
                                            <!-- DISPLAY MODE -->
                                            {#if hasRealGuess}
                                                <div
                                                    class="score-display"
                                                    style="background: rgba(0,0,0,0.2); padding: 0.25rem 0.75rem; border-radius: var(--radius-md);"
                                                >
                                                    {item.home_score} - {item.away_score}
                                                </div>
                                            {:else}
                                                <div
                                                    style="padding: 0.5rem; background: rgba(255,255,255,0.05); border-radius: var(--radius-sm); color: var(--color-text-muted); font-size: 0.875rem;"
                                                >
                                                    Sem palpite
                                                </div>
                                            {/if}
                                        {/if}
                                    </div>

                                    <!-- Away Team -->
                                    <div
                                        style="flex: 1; text-align: left; padding-left: 1.5rem; min-width: 0;"
                                    >
                                        <div style="font-weight: 700; font-size: 1.25rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                            {item.match.away_team?.name ?? "A definir"}
                                        </div>
                                        <div
                                            style="color: var(--color-text-muted); font-size: 0.875rem; display: flex; align-items: center; justify-content: flex-start; gap: 0.375rem;"
                                        >
                                            {#if item.match.away_team?.crest}
                                                <img
                                                    src={item.match.away_team.crest}
                                                    alt={item.match.away_team?.name ?? "A definir"}
                                                    style="width: 1.25rem; height: 1.25rem; object-fit: contain;"
                                                />
                                            {/if}
                                            <span>{item.match.away_team?.code ?? "TBD"}</span>
                                        </div>
                                    </div>
                                </div>

                                <!-- Footer: Action buttons and points -->
                                <div
                                    style="margin-top: 1.5rem; display: flex; justify-content: space-between; align-items: center;"
                                >
                                    <!-- Match Result / Points -->
                                    <div
                                        style="font-size: 0.875rem; color: var(--color-text-muted);"
                                    >
                                        {#if item.match.status === "FINISHED"}
                                            Pontos ganhos:
                                            <strong
                                                style="color: var(--color-accent); font-size: 1.1rem;"
                                            >
                                                {item.points !== null ? item.points : 0}
                                            </strong>
                                        {:else if item.match.status === "IN_PLAY"}
                                            Placar atual: {item.match.home_score ?? 0} x {item
                                                .match.away_score ?? 0}
                                        {/if}
                                    </div>

                                    <!-- Action Buttons -->
                                    <div>
                                        {#if editingMatchId === item.match.id}
                                            {#if submitError}
                                                <div
                                                    style="color: var(--color-danger); font-size: 0.75rem; text-align: right; margin-bottom: 0.5rem;"
                                                >
                                                    {submitError}
                                                </div>
                                            {/if}
                                            <div
                                                style="display: flex; gap: 0.5rem; justify-content: flex-end;"
                                            >
                                                <button
                                                    class="btn"
                                                    style="background: transparent; color: var(--color-text-muted); padding: 0.5rem 1rem;"
                                                    onclick={cancelEdit}
                                                    disabled={isSubmitting}>Cancelar</button
                                                >
                                                <button
                                                    class="btn btn-primary"
                                                    style="padding: 0.5rem 1rem;"
                                                    onclick={() =>
                                                        handleSubmitGuess(item.match.id)}
                                                    disabled={isSubmitting}
                                                >
                                                    {isSubmitting ? "Salvando..." : "Salvar"}
                                                </button>
                                            </div>
                                        {:else if isEditable}
                                            <button
                                                class="btn"
                                                style="background: {hasRealGuess
                                                    ? 'transparent'
                                                    : 'var(--color-primary)'}; color: {hasRealGuess
                                                    ? 'var(--color-primary)'
                                                    : 'white'}; border: 1px solid var(--color-primary); padding: 0.5rem 1.5rem;"
                                                onclick={() =>
                                                    startEdit(
                                                        item.match.id,
                                                        item.home_score,
                                                        item.away_score,
                                                    )}
                                            >
                                                {hasRealGuess ? "Alterar Palpite" : "Fazer Palpite"}
                                            </button>
                                        {/if}
                                    </div>
                                </div>
                            </div>
                        {/each}
                    </div>
                </div>
            {/each}
        </div>
    {/if}
</div>

<style>
    .date-group {
        display: flex;
        flex-direction: column;
        gap: 1rem;
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
