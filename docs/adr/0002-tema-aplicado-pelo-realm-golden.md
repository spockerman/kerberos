# 0002 — Tema aplicado pelo realm golden, não como padrão global

- Status: aceita (2026-11-24) — substitui o item 6 original do CLAUDE.md ("tema como padrão do servidor")

## Contexto
O Keycloak tem a opção `spi-theme--default`, mas ela vale para todos os tipos de tema (login, conta, admin, e-mail).
O tema `kerberos` implementa só as telas de login; usar o padrão global quebraria o console de administração.

## Decisão
O módulo `realm-golden` define `login_theme = "kerberos"`. Como todo realm é criado por esse módulo,
o efeito é o mesmo: nenhum realm nasce sem o layout da empresa.

## Consequências
- Realms criados fora do Terraform (proibido pela regra de ouro) ficariam com o tema padrão.
- Quando o tema ganhar conta/e-mail, basta adicionar `account_theme`/`email_theme` ao módulo.
