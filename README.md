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
| **Portal de provisionamento** | http://localhost:3001 | `operador` / `Senha@Demo2026!` |
| Aplicação de exemplo | http://localhost:3000 | usuários abaixo |
| Keycloak (admin) | http://localhost:8080/admin | `admin` / `admin` |
| API de exemplo | http://localhost:8081/api/me | precisa de token |
| API do portal | http://localhost:8082/api/systems | precisa de token do realm `plataforma` |
| E-mails enviados (Mailpit) | http://localhost:8025 | — |

### Usuários de teste (realm `demo`)

Senha de todos: `Senha@Demo2026!` (só ambiente local).

| Usuário | Organização (tenant) | Role |
|---|---|---|
| `ana` | Acme | admin |
| `bruno` | Globex | usuario (padrão) |
| `carla` | Acme **e** Globex (escolhe no login) | gestor |
| `diego@initech.test` | Initech — entra pelo **IdP do cliente** (OIDC simulado) | usuario |
| `eva` | Umbrella — entra pelo **LDAP do cliente** (OpenLDAP simulado) | usuario |

Initech e Umbrella são clientes simulados para testar a federação: o IdP é o realm `initech-idp` no mesmo
Keycloak e o LDAP é o container `ldap-umbrella`. Em produção, ficam no ambiente do cliente.

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
KC_LOG_LEVEL="info,org.keycloak.storage.ldap:debug" docker compose up -d keycloak  # logs detalhados de uma área
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
| `infra/terraform/modules/tenant/` | Cliente (tenant): Organization + IdP OIDC/SAML ou LDAP |
| `infra/terraform/envs/local/` | Ambiente local: realm `demo`, clientes e usuários de teste |
| `keycloak/providers/` | Extensões Java do Keycloak (mapper LDAP → Organization) |
| `apps/demo-api/` | API Spring Boot em Ports and Adapters |
| `apps/demo-web/` | Front-end Next.js com login OIDC |
| `portal/api/` | API do portal (Spring Boot, Ports and Adapters, Admin API do Keycloak, Postgres) |
| `portal/web/` | Front do portal (Next.js) |
| `scripts/smoke-test.py` | Teste ponta a ponta |

## Portal de provisionamento

Em http://localhost:3001, entre com `operador` (tem a role `provisionador`; `visitante` entra mas não provisiona).

1. **Novo sistema**: nome, endereço do front-end, roles extras e se é multi-tenant. O portal cria o realm com o
   template da plataforma (tema, senha, força bruta, eventos, Organizations), o client do front (PKCE) e o audience da API.
2. **.env do projeto**: na página do sistema, copie ou baixe. O time cola no projeto e começa pelas telas.
3. **Clientes**: cadastre cada cliente com o IdP dele (OIDC), o LDAP dele ou usuários locais. Para OIDC, a página mostra a
   URL de retorno a cadastrar no IdP do cliente.
4. **Auditoria**: tudo o que foi feito, por quem e com qual resultado.

Repetir um cadastro é seguro: o portal confere o que existe e só cria ou corrige o que faltar.

## Cadastrar um cliente pelo Terraform (base da plataforma)

Clientes dos sistemas criados pelo portal são cadastrados no portal. Para realms geridos pelo Terraform (como o `demo`):

Cada cliente é um bloco do módulo `tenant` no ambiente (ex.: `infra/terraform/envs/local/main.tf`):

```hcl
# Cliente com Microsoft Entra ID (OIDC)
module "tenant_contoso" {
  source     = "../../modules/tenant"
  realm_id   = module.realm.realm_id
  realm_name = module.realm.realm_name
  alias      = "contoso"            # vira o claim `organization`
  name       = "Contoso"
  domains    = ["contoso.com"]      # quem digitar @contoso.com vai direto para o Entra ID

  identity_provider = {
    type              = "oidc"
    issuer            = "https://login.microsoftonline.com/<tenant-id>/v2.0"
    authorization_url = "https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/authorize"
    token_url         = "https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token"
    jwks_url          = "https://login.microsoftonline.com/<tenant-id>/discovery/v2.0/keys"
    client_id         = "<application-id>"
  }
  identity_provider_client_secret = var.contoso_client_secret
}
```

No app registrado no IdP do cliente, a URL de retorno é
`<keycloak>/realms/<realm>/broker/<alias>-idp/endpoint`. Para SAML use `type = "saml"` com `entity_id`,
`single_sign_on_service_url` e `signing_certificate`; para LDAP/AD, o bloco `ldap` (veja as descrições em
`infra/terraform/modules/tenant/variables.tf`).

## Desenvolver o tema

```bash
cd theme && npm install && npm run dev
```

Abre a tela de login com dados simulados em http://localhost:5173 (outras telas: `?page=login-update-password.ftl`).
Para ver no Keycloak de verdade: `docker compose up --build keycloak`.
