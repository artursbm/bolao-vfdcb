<script lang="ts">
</script>

<div class="container" style="padding-top: 4rem; padding-bottom: 4rem;">
    <header style="margin-bottom: 3rem; text-align: center;">
        <h1 class="animate-fade-in" style="font-size: 3rem;">Regras do Bolão</h1>
        <p class="animate-fade-in" style="font-size: 1.125rem; color: var(--color-text-muted);">
            Entenda como funciona a pontuação do Bolão da Copa do Mundo 2026.
        </p>
    </header>

    <div class="card animate-fade-in" style="padding: 2rem; max-width: 800px; margin: 0 auto; animation-delay: 0.1s;">
        <h2 style="margin-bottom: 1.5rem; color: var(--color-primary);">Sistema de Pontuação</h2>
        
        <div style="display: flex; flex-direction: column; gap: 1.5rem;">
            <div class="rule-item">
                <div class="rule-points">4 Pontos</div>
                <div class="rule-desc">
                    <strong>Placar Exato:</strong> Você acertou o vencedor e o placar exato da partida.
                    <br/><span style="font-size: 0.875rem; color: var(--color-text-muted);">Ex: Seu palpite 2x1, Placar real 2x1.</span>
                </div>
            </div>

            <div class="rule-item">
                <div class="rule-points">3 Pontos</div>
                <div class="rule-desc">
                    <strong>Vencedor e Saldo:</strong> Você acertou o vencedor e a diferença de gols (saldo), mas errou o placar exato.
                    <br/><span style="font-size: 0.875rem; color: var(--color-text-muted);">Ex: Seu palpite 2x0, Placar real 3x1 (diferença de 2 gols para o mesmo vencedor).</span>
                </div>
            </div>

            <div class="rule-item">
                <div class="rule-points">2 Pontos</div>
                <div class="rule-desc">
                    <strong>Vencedor:</strong> Você acertou apenas o time vencedor, mas errou a diferença de gols e o placar.
                    <br/><span style="font-size: 0.875rem; color: var(--color-text-muted);">Ex: Seu palpite 1x0, Placar real 3x0.</span>
                </div>
            </div>

            <div class="rule-item">
                <div class="rule-points">1 Ponto</div>
                <div class="rule-desc">
                    <strong>Empate:</strong> Você acertou que a partida terminaria em empate, mas errou o placar exato.
                    <br/><span style="font-size: 0.875rem; color: var(--color-text-muted);">Ex: Seu palpite 1x1, Placar real 0x0.</span>
                </div>
            </div>

            <div class="rule-item">
                <div class="rule-points" style="color: var(--color-danger);">0 Pontos</div>
                <div class="rule-desc">
                    <strong>Erro:</strong> Você errou o resultado da partida (vencedor ou empate).
                </div>
            </div>
        </div>

        <h2 style="margin-top: 3rem; margin-bottom: 1.5rem; color: var(--color-primary);">Outras Informações</h2>
        <ul style="color: var(--color-text); line-height: 1.6; padding-left: 1.5rem;">
            <li>Os palpites podem ser alterados livremente até o horário de início oficial de cada partida.</li>
            <li>Após o início do jogo, o palpite é bloqueado e não pode mais ser alterado.</li>
            <li>O ranking é atualizado automaticamente ao término de cada partida.</li>
        </ul>
    </div>
</div>

<style>
    .rule-item {
        display: flex;
        align-items: flex-start;
        gap: 1.5rem;
        padding: 1.5rem;
        background: rgba(255, 255, 255, 0.03);
        border-radius: var(--radius-md);
        border-left: 4px solid var(--color-primary);
    }
    
    .rule-points {
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--color-accent);
        min-width: 100px;
    }
    
    .rule-desc {
        flex: 1;
        line-height: 1.5;
    }

    @media (max-width: 640px) {
        .rule-item {
            flex-direction: column;
            gap: 0.5rem;
        }
    }
</style>
