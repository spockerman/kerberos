# Kerberos — Plataforma de Identidade

Keycloak configurado uma vez, como código, para que cada sistema novo já nasça com login,
autorização e multi-tenancy (SaaS) prontos, no layout da empresa.

Diretrizes de desenvolvimento: [`CLAUDE.md`](CLAUDE.md).

## Rodar localmente

Único pré-requisito: **Docker** (Docker Desktop no Mac).

```bash
git clone https://github.com/spockerman/kerberos.git
cd kerberos
docker compose up --build
```

A primeira vez demora alguns minutos (baixa imagens e compila tema, API e front-end).
Quando o serviço `terraform` terminar com `Apply complete!`, está tudo pronto:

| O quê | Endereço | Acesso |
|---|---|---|
| Aplicação de exemplo | http://localhost:3000 | usuários abaixo |
| Keycloak (admin) | http://localhost:8080/admin | `admin` / `admin` |
| API de exemplo | http://localhost:8081/api/me | precisa de token |
| E-mails enviados (Mailpit) | http://localhost:8025 | — |

### Usuários de teste (realm `demo`)

Senha de todos: `Senha@Demo2026!` (só ambiente local).

| Usuário | Organização (tenant) | Role |
|---|---|---|
| `ana` | Acme | admin |
| `bruno` | Globex | usuario (padrão) |
| `carla` | Acme **e** Globex (escolhe no login) | gestor |

### Conferir tudo de uma vez

Com o ambiente no ar:

```bash
python3 scripts/smoke-test.py
```

Faz o login real pelo tema, valida o token (tenant, audience, roles) e o isolamento de tenant na API.
É o mesmo teste que roda no CI a cada push.

### Comandos úteis

```bash
docker compose up --build -d     # sobe em segundo plano
docker compose logs -f keycloak  # acompanha os logs
docker compose run --rm terraform  # reaplica o Terraform depois de mudar infra/terraform
docker compose down              # para (mantém os dados)
docker compose down -v           # para e apaga banco e state (recomeça do zero)
```

## O que tem aqui

| Pasta | Conteúdo |
|---|---|
| `keycloak/` | Imagem do Keycloak 26.8.2 com o tema embutido |
| `theme/` | Tema das telas de login (Keycloakify + tokens do design system) |
| `design-system/` | Tokens visuais, fonte e identidade (`brand/`) |
| `infra/terraform/modules/realm-golden/` | Template aplicado a todo sistema novo |
| `infra/terraform/modules/spa-app/` | Client de uma SPA (PKCE + audience da API) |
| `infra/terraform/envs/local/` | Ambiente local: realm `demo`, organizations e usuários de teste |
| `apps/demo-api/` | API Spring Boot em Ports and Adapters |
| `apps/demo-web/` | Front-end Next.js com login OIDC |
| `scripts/smoke-test.py` | Teste ponta a ponta |

## Desenvolver o tema

```bash
cd theme && npm install && npm run dev
```

Abre a tela de login com dados simulados em http://localhost:5173 (outras telas: `?page=login-update-password.ftl`).
Para ver no Keycloak de verdade: `docker compose up --build keycloak`.
