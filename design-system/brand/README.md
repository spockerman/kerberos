# Marca — Central 156

Conceito aprovado: **6 · Tipográfico** — o número 156 é a marca; os três pontos assinam os canais de atendimento
(verde-água = telefone, azul = internet, âmbar = aplicativo). Origem: `Central 156 - Logos.html` (Claude Design).

| Arquivo | Uso | Tamanho mínimo |
|---|---|---|
| `logo.svg` | Logo completo ("CENTRAL" + 156 + pontos) em fundo claro ou no gradiente | **48 px de altura** — abaixo disso "CENTRAL" fica ilegível |
| `logo-inverse.svg` | Logo completo em fundo escuro (navy) | 48 px de altura |
| `logo-mark.svg` | Marca compacta (156 + pontos): barras, cabeçalhos, cards | 20 px de altura |
| `app-icon.svg` | Ícone do app e favicon (quadrado navy, cantos arredondados) | 16 px (os pontos somem abaixo de 24 px; o 156 continua legível) |

## Regras
- Os SVGs têm o texto **convertido em contorno**: não dependem da fonte Sora estar instalada.
- Cores da marca (só no logo, não na interface): tinta `#0E1B3A`, cinza `#5B6B8C`, pontos `#14C2B3`, `#2E8FFF`, `#FFB020`.
  A interface continua com os tokens de `design-system/tokens.css`.
- Não recolorir, esticar, nem separar os pontos do 156. Área de respiro: no mínimo a altura de um ponto em volta.
- Para mudar o logo, edite `source/build_logo.py` e rode `python3 source/build_logo.py` (precisa de `pip install fonttools brotli`).

`source/Sora-latin-variable.woff2`: fonte Sora (SIL Open Font License 1.1), usada só para gerar os contornos.
