<script lang="ts">
    import { signup } from "$lib/auth";
    import { goto } from "$app/navigation";

    let name = "";
    let email = "";
    let password = "";
    let confirmPassword = "";
    let errorMsg = "";
    let loading = false;

    async function handleSubmit(event: Event) {
        event.preventDefault();
        errorMsg = "";

        if (password !== confirmPassword) {
            errorMsg = "As senhas não coincidem";
            return;
        }

        loading = true;
        try {
            await signup({ name, email, password });
            goto("/"); // Redirect to home page on success
        } catch (err: any) {
            errorMsg = err.message || "Falha no cadastro. Tente novamente.";
        } finally {
            loading = false;
        }
    }
</script>

<div class="auth-container">
    <div class="auth-card">
        <div class="auth-header">
            <h1>Criar uma Conta</h1>
            <p>Participe do Bolão VDCB</p>
        </div>

        {#if errorMsg}
            <div class="error-alert">
                {errorMsg}
            </div>
        {/if}

        <form onsubmit={handleSubmit} class="auth-form">
            <div class="form-group">
                <label for="name">Nome Completo</label>
                <input
                    type="text"
                    id="name"
                    bind:value={name}
                    required
                    placeholder="João Silva"
                    autocomplete="name"
                />
            </div>

            <div class="form-group">
                <label for="email">e-mail</label>
                <input
                    type="email"
                    id="email"
                    bind:value={email}
                    required
                    placeholder="seu@e-mail.com"
                    autocomplete="email"
                />
            </div>

            <div class="form-group">
                <label for="password">Senha</label>
                <input
                    type="password"
                    id="password"
                    bind:value={password}
                    required
                    minlength="8"
                    placeholder="••••••••"
                    autocomplete="new-password"
                />
                <small class="hint">Mínimo de 8 caracteres</small>
            </div>

            <div class="form-group">
                <label for="confirmPassword">Confirmar Senha</label>
                <input
                    type="password"
                    id="confirmPassword"
                    bind:value={confirmPassword}
                    required
                    placeholder="••••••••"
                    autocomplete="new-password"
                />
            </div>

            <button type="submit" class="btn-submit" disabled={loading}>
                {loading ? "Criando conta..." : "Cadastrar"}
            </button>
        </form>

        <div class="auth-footer">
            <p>Já tem uma conta? <a href="/login">Entrar</a></p>
        </div>
    </div>
</div>

<style>
    .auth-container {
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: calc(100vh - 150px);
        padding: 2rem 0;
    }

    .auth-card {
        background: var(--color-surface);
        padding: 2.5rem;
        border-radius: var(--radius-lg);
        border: 1px solid var(--color-border);
        box-shadow: var(--shadow-lg);
        width: 100%;
        max-width: 450px;
    }

    .auth-header {
        text-align: center;
        margin-bottom: 2rem;
    }

    .auth-header h1 {
        font-size: 1.875rem;
        font-family: var(--font-heading);
        font-weight: bold;
        color: var(--color-text);
        margin-bottom: 0.5rem;
    }

    .auth-header p {
        color: var(--color-text-muted);
    }

    .error-alert {
        background-color: rgba(239, 68, 68, 0.1);
        color: var(--color-danger);
        border: 1px solid rgba(239, 68, 68, 0.2);
        padding: 0.75rem;
        border-radius: var(--radius-md);
        margin-bottom: 1.5rem;
        font-size: 0.875rem;
        text-align: center;
    }

    .auth-form {
        display: flex;
        flex-direction: column;
        gap: 1.25rem;
    }

    .form-group {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }

    .form-group label {
        font-size: 0.875rem;
        font-weight: 500;
        color: var(--color-text-muted);
    }

    .form-group input {
        padding: 0.75rem;
        background-color: var(--color-bg);
        border: 1px solid var(--color-border);
        color: var(--color-text);
        border-radius: var(--radius-md);
        outline: none;
        transition:
            border-color 0.2s,
            box-shadow 0.2s;
    }

    .form-group input:focus {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
    }

    .hint {
        font-size: 0.75rem;
        color: var(--color-text-muted);
        margin-top: 0.25rem;
    }

    .btn-submit {
        background: linear-gradient(
            135deg,
            var(--color-primary),
            var(--color-primary-hover)
        );
        color: white;
        padding: 0.875rem;
        border: none;
        border-radius: var(--radius-md);
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        margin-top: 0.5rem;
    }

    .btn-submit:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: var(--shadow-md);
    }

    .btn-submit:disabled {
        opacity: 0.7;
        cursor: not-allowed;
        transform: none;
    }

    .auth-footer {
        margin-top: 2rem;
        text-align: center;
        font-size: 0.875rem;
        color: var(--color-text-muted);
    }

    .auth-footer a {
        color: var(--color-primary);
        text-decoration: none;
        font-weight: 500;
    }

    .auth-footer a:hover {
        color: var(--color-primary-hover);
        text-decoration: underline;
    }
</style>
