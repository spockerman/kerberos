# Design system

Fonte visual da plataforma. Origem: `Loanza Design System.html` (raiz do repo), tema claro apenas.

> **O HTML é só referência de estilo.** "Loanza" e seu logo pertencem ao estudo de caso de outra empresa
> (Rondesignlab) e **nunca** aparecem no produto, no código, em assets ou em textos.
> A identidade da plataforma é **Kerberos** (nome, logo, favicon) e vive apenas em `brand/`.

- `brand/` — nome, logo e favicon do sistema (`brand.json`). **Único lugar com identidade**; o resto do código lê daqui.
- `tokens.css` — variáveis CSS. Única fonte de cores, tipografia, espaço, raio, sombra e movimento.
- Consumido pelo tema Keycloakify (`theme/`) e pelos front-ends Next.js.

## Regras que valem para todo componente
- **Fonte:** Outfit (pesos 200–500), servida localmente (sem Google Fonts em runtime). Licença SIL OFL.
- **Pills para o que se clica, retângulos suaves para o que se lê.** Botões e status são pill; cards e inputs não.
- **Fill não é cor de texto.** Verde `#5DD644` e laranja `#FE6A3A` só em ponto, barra e pill. Texto usa `--color-positive-ink` / `--color-alert-ink`.
- **Erro de formulário:** borda e mensagem em `--color-alert-ink`, nunca o laranja.
- **Gradiente e tile invertido (fundo ink):** no máximo um de cada por tela.
- **Profundidade por tom**, não por borda: ground → surface → card → overlay.
- **Card não aninha filho com o mesmo raio:** desça um nível (card 20 → tile 16 → input 12).
- **Foco sempre visível:** `--focus-ring`. Alvo de toque mínimo 44×44. Botão só com ícone exige `aria-label`.
- **Movimento:** só `transform` e `opacity`, respeitando `prefers-reduced-motion`.
- **Números:** `font-variant-numeric: tabular-nums`.

## Mapa para a tela de login (Keycloak)
| Elemento | Token |
|---|---|
| Fundo da página | `--color-ground` (ou `--gradient-accent` como faixa/hero) |
| Card do formulário | `--color-card`, `--radius-panel`, `--shadow-card`, padding `--space-12` |
| Logo e nome do sistema | `brand/brand.json` (`logo`, `productName`) — nunca fixos no tema |
| Título | `--text-title` |
| Label do campo | `--text-caption`, `--color-ink-muted` |
| Input | `--input-height`, `--radius-input`, `--input-border` / `-focus` / `-error` |
| Botão "Entrar" | pill `--color-ink`, texto `--color-on-ink`, altura `--button-height` |
| Botões de IdP (Organizations) | pill outline: borda 1px `--color-ink`, fundo transparente |
| Links ("Esqueci a senha") | `--color-ink`, hover `--color-ink-muted` |
| Mensagem de erro | `--color-alert-ink` sobre `--color-alert-tint` |
