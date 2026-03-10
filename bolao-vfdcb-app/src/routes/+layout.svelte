<script lang="ts">
	import "../app.css";
	import favicon from "$lib/assets/favicon.svg";
	import { onMount } from "svelte";
	import { user, checkAuth, logout } from "$lib/auth";

	let { children } = $props();

	onMount(() => {
		checkAuth();
	});

	async function handleLogout() {
		await logout();
		// Optional: redirect to home or login page
		window.location.href = "/login";
	}
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<title>Bolão VFDCB</title>
</svelte:head>

<div class="app-container">
	<nav class="navbar">
		<div class="navbar-left">
			<div class="navbar-brand">
				<a href="/">Bolão VFDCB</a>
			</div>
			{#if $user}
				<a href="/ranking" class="nav-link">Ranking</a>
			{/if}
		</div>
		<div class="navbar-menu">
			{#if $user}
				<a href="/guesses" class="nav-link">Meus Palpites</a>
				<span class="user-greeting">Olá, {$user.name}</span>
				<button class="btn-logout" onclick={handleLogout}>Sair</button>
			{:else}
				<a href="/login" class="nav-link">Entrar</a>
				<a href="/signup" class="btn-primary">Cadastrar</a>
			{/if}
		</div>
	</nav>

	<main class="main-content">
		{@render children()}
	</main>
</div>

<style>
	.app-container {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
	}

	.navbar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 1rem 2rem;
		background-color: var(--color-surface);
		border-bottom: 1px solid var(--color-border);
		box-shadow: var(--shadow-sm);
	}

	.navbar-left {
		display: flex;
		align-items: center;
		gap: 2rem;
	}

	.navbar-brand a {
		font-size: 1.5rem;
		font-weight: bold;
		color: var(--color-primary);
		text-decoration: none;
		font-family: var(--font-heading);
	}

	.navbar-menu {
		display: flex;
		align-items: center;
		gap: 1.5rem;
	}

	.nav-link {
		color: var(--color-text-muted);
		text-decoration: none;
		font-weight: 500;
		transition: color 0.2s;
	}

	.nav-link:hover {
		color: var(--color-text);
	}

	.btn-primary {
		padding: 0.5rem 1rem;
		border-radius: var(--radius-md);
		text-decoration: none;
		font-weight: 500;
		transition: all 0.2s;
	}

	.btn-logout {
		background-color: transparent;
		border: 1px solid var(--color-border);
		color: var(--color-text-muted);
		padding: 0.5rem 1rem;
		border-radius: var(--radius-md);
		cursor: pointer;
		font-weight: 500;
		transition: all 0.2s;
	}

	.btn-logout:hover {
		background-color: var(--color-surface-hover);
		color: var(--color-text);
	}

	.user-greeting {
		color: var(--color-text-muted);
		font-weight: 500;
	}

	.main-content {
		flex: 1;
		width: 100%;
	}
</style>
