# CLAUDE.md — Plataforma de Identidade Keycloak

Diretrizes para quem (pessoa ou agente) desenvolve neste repositório. Leia antes de escrever código.
Idioma do projeto: **português do Brasil** (docs, commits, comentários). Nomes de código e recursos técnicos em inglês.

---

## 1. Propósito

Configurar o Keycloak **uma única vez** e permitir que cada sistema novo receba autenticação, autorização
e multi-tenancy (SaaS) prontos, com **poucos campos** num portal web, mantendo o **layout da empresa**.
O time de produto deve focar apenas em telas e regras de negócio.

Regra de ouro: **nenhuma configuração manual no console do Keycloak.** Se algo foi clicado no admin console,
está errado — deve existir como código neste repositório.

---

## 2. Arquitetura (visão geral)

```
Portal de Provisionamento ──▶ Admin REST API / pipeline Terraform ──▶ Keycloak central
        │                                                            ├─ realm sistema-A ← template golden
        └──▶ entrega client_id, secret e .env ao projeto            │    ├─ Organization: Cliente X (+ IdP)
                                                                     │    └─ Organization: Cliente Y (+ LDAP)
Aplicações (starters internos) ── login OIDC ───────────────────────▶└─ realm sistema-B ← template golden
```

Componentes:

| Componente | Responsabilidade | Pasta |
|---|---|---|
| Imagem Docker corporativa | Keycloak oficial + tema + providers, já com `kc.sh build` | `keycloak/` |
| Providers próprios | Extensões Java do Keycloak (ex.: mapper LDAP → Organization) | `keycloak/providers/` |
| Design system | Tokens visuais (`tokens.css`), fonte Outfit e regras de uso | `design-system/` |
| Tema (Keycloakify) | Telas de login no layout da empresa (tema `kerberos`) | `theme/` |
| Realm golden | Template de realm em Terraform (tema, senha, brute force, tokens, roles, eventos, SMTP, Organizations) | `infra/terraform/modules/realm-golden/` |
| Client de SPA | Módulo de client público (PKCE + audience da API + escopo `organization`) | `infra/terraform/modules/spa-app/` |
| Tenant (cliente) | Organization + login do cliente: IdP OIDC/SAML ou LDAP | `infra/terraform/modules/tenant/` |
| Clientes simulados | IdP e LDAP falsos para testar a federação localmente | `infra/terraform/envs/local/simulated-customers.tf`, `infra/local/` |
| Ambientes | Composição dos módulos por ambiente (dev, hml, prd) | `infra/terraform/envs/` |
| Portal de Provisionamento | `portal/api` (Spring Boot) + `portal/web` (Next.js): cria sistemas e clientes pela Admin API, gera o `.env`, audita | `portal/` |
| Realm da plataforma | Realm `plataforma` (login do portal, role `provisionador`) e conta de serviço `portal-provisioner` | `infra/terraform/envs/local/platform.tf` |
| Starters/SDKs | Bibliotecas que validam JWT, extraem tenant e checam roles | `starters/` |
| Ambiente local | `docker-compose.yml` na raiz: Postgres, Keycloak, Terraform, Mailpit e app de exemplo | raiz |
| App de exemplo | API Spring Boot (`demo-api`) e front Next.js (`demo-web`) que provam o fluxo ponta a ponta | `apps/` |
| Testes ponta a ponta | Login real pelo tema + validação de token e API | `scripts/smoke-test.py` |
| CI | Builds, testes, ArchUnit, `terraform validate` e teste ponta a ponta a cada push | `.github/workflows/` |
| Decisões | ADRs | `docs/adr/` |

> Ao criar uma pasta nova de primeiro nível, atualize esta tabela.

---

## 3. Decisões fundamentais (não mudar sem ADR)

1. **Um Keycloak central** (cluster + Postgres), não um Keycloak por projeto.
2. **Um realm por sistema/produto. Uma Organization por cliente (tenant).**
   Nunca criar realm por tenant — degrada desempenho e administração com centenas de realms.
3. **Multi-tenancy via Keycloak Organizations** (Keycloak 26+). O token carrega o claim `organization`;
   as aplicações filtram dados por ele.
4. **Federação por cliente** fica na Organization (IdP próprio: Entra ID, Google Workspace, SAML) ou via
   User Federation (LDAP/AD).
5. **Configuração como código**: Terraform (provider oficial `keycloak/keycloak`) é a fonte da verdade da **base da
   plataforma** (template golden, realm `plataforma`, conta do portal, ambientes). **Sistemas e clientes criados pelo
   portal** são geridos pelo portal via Admin API, com catálogo e auditoria no banco dele (ADR 0004).
   Exports JSON do console servem só como referência, nunca como mecanismo de deploy.
6. **Tema aplicado pelo template**: o realm golden define `login_theme = "kerberos"`, então todo realm nasce
   com o layout da empresa, sem passo extra. (Não usamos o padrão global do servidor: ele vale para todos os
   tipos de tema — admin, conta — e o nosso só implementa login.)
7. **Portal usa service account** (`portal-provisioner`) com o mínimo: só `create-realm` no `master` (o Keycloak dá a ela
   a administração de cada realm que ela cria) e o escopo `kc-admin-api`. Nunca o usuário admin do `master`.
8. **Arquitetura hexagonal (Ports and Adapters)** em todo código de aplicação (portal e starters). Ver seção 6.
9. **Produção na AWS com custo mínimo** enquanto não houver exigência de alta disponibilidade. Ver seção 5.

Mudar qualquer item acima exige um ADR em `docs/adr/NNNN-titulo.md` (contexto, decisão, consequências).

---

## 4. Roteiro e estado atual

| Fase | Entrega | Status |
|---|---|---|
| 1 | Imagem Docker + tema + realm golden + ambiente local (v0.1) | ✅ concluída — CI ponta a ponta verde |
| 2 | Organizations + federação por cliente (módulo `tenant`: IdP OIDC/SAML e LDAP) | ✅ concluída — OIDC e LDAP testados ponta a ponta; SAML sem teste automatizado |
| 3 | Portal de provisionamento (sistemas, clientes OIDC/LDAP/locais, `.env`, auditoria) | ✅ concluída — criação pelo portal e login no sistema novo testados ponta a ponta |
| 4 | Starters e SDKs das stacks | ⬜ |

Atualize esta tabela ao concluir cada fase. Não adiante fases: construa sobre o que já está estável.

---

## 5. Stacks e infraestrutura

| Item | Decisão |
|---|---|
| Keycloak | **26.8.2** — versão fixada só em `keycloak/Dockerfile` (`ARG KEYCLOAK_VERSION`) |
| Back-end (portal e sistemas) | **Java + Spring Boot** |
| Front-end (portal e sistemas) | **React / Next.js** |
| Tema de login | **Keycloakify** (React), usando os tokens de `design-system/tokens.css` |
| Design system | `Loanza Design System.html` (raiz) → extraído para `design-system/`. Tema claro apenas, fonte Outfit. **Só referência de estilo** |
| Identidade (nome, logo) | **Central 156** — conceito 6 · Tipográfico. Arquivos e regras de uso em `design-system/brand/` (logo completo só a partir de 48 px; abaixo, `logo-mark.svg`) |
| Configuração do Keycloak | **Terraform 1.17.2** (imagem Docker, sem instalação local) + provider `keycloak/keycloak` `~> 5.11` |
| Versões das apps | Spring Boot 4.1 + Java 21 · Next.js 16 + React 19 · Keycloakify 11 (React 18) · Node 22 |
| Pré-requisito local | **Só Docker.** Tudo (inclusive Terraform, Maven e Node) roda em containers |
| Banco | **PostgreSQL** — um para o Keycloak, outro para o portal (`portal-db`, migrações com **Flyway**, acesso com `JdbcClient`) |
| Produção | **AWS Lightsail** — instância de 4 GB rodando docker-compose (Keycloak + Postgres + proxy reverso com TLS), snapshots automáticos |
| Infra da AWS como código | Terraform (provider `aws`), separado do Terraform do Keycloak |

Motivo do Lightsail: é a opção mais barata na AWS com preço mensal fixo. O Keycloak usa cerca de 1,25 GB
de RAM por instância; somado ao Postgres e ao sistema operacional, 4 GB é o mínimo confortável.
**Caminho de evolução** (exige ADR): quando precisar de alta disponibilidade, migrar para EC2/ECS com
2+ instâncias e RDS PostgreSQL. Como tudo é código e containers, a migração não muda a configuração do Keycloak.

Não introduzir linguagem, framework ou ferramenta nova sem registrar aqui.

---

## 6. Arquitetura de código: Ports and Adapters (hexagonal)

Vale para o **portal** e para os **starters/sistemas** em Java/Spring Boot. O front-end segue a mesma ideia
de forma leve: regras e chamadas de API fora dos componentes de tela.

### Regras
- **Dependências sempre apontam para dentro**: adapters → application → domain. O domínio não depende de nada externo.
- **Domínio puro**: sem imports de Spring, JPA, Jackson, Keycloak ou SDKs. Só Java e tipos do próprio domínio.
- **Todo efeito colateral é uma porta de saída** (outbound port), nomeada pela capacidade, não pela tecnologia:
  `IdentityProvisioningPort`, não `KeycloakClient`; `ProvisioningAuditPort`, não `AuditJpaRepository`.
- **Keycloak é um adapter**, nunca o centro. O portal fala com `IdentityProvisioningPort`; a implementação
  com Admin REST API ou pipeline Terraform fica em `adapter.out.keycloak` / `adapter.out.terraform`.
- **Nos sistemas**, o usuário autenticado chega ao domínio como um tipo próprio (ex.: `CurrentUser` com
  `tenantId` e `roles`), entregue por uma porta. O domínio nunca vê `Jwt`, `Authentication` ou claims crus.
- **Controllers são finos**: convertem HTTP → input do caso de uso → HTTP. Sem regra de negócio.
- **Erros são traduzidos** na fronteira: exceção de infraestrutura vira erro de aplicação/domínio.
- **Wiring explícito** num composition root (classes `@Configuration`); casos de uso não precisam de anotação Spring.

### Estrutura de pacotes (por feature)
```
<base>.<feature>/              # base atual: br.kerberos.<app> (ex.: br.kerberos.demo.me)
  domain/                      # entidades, value objects, regras
  application/
    port/in/                   # interfaces dos casos de uso (comandos/consultas)
    port/out/                  # interfaces das dependências externas
    usecase/                   # implementação dos casos de uso
  adapter/
    in/web/                    # controllers REST, DTOs de request/response
    out/keycloak/              # Admin REST API
    out/terraform/             # disparo de pipeline
    out/persistence/           # JPA/Postgres
  config/                      # composition root
```

### Testes por fronteira
- Domínio: testes puros, sem mocks nem Spring.
- Casos de uso: fakes em memória das portas de saída.
- Adapters de saída: testes de integração com infraestrutura real (Testcontainers com Keycloak e Postgres).
- Adapters de entrada: mapeamento HTTP ↔ caso de uso.
- Regras de dependência verificadas automaticamente com **ArchUnit** (o build falha se o domínio importar framework).

---

## 7. Convenções

### Nomes
- Realm: `<sistema>` em kebab-case (ex.: `financeiro`, `ead-portal`).
- Client: `<sistema>-<tipo>` → `-web` (SPA, público + PKCE), `-api` (bearer-only/confidencial), `-mobile`, `-svc` (service account).
- Organization: alias estável do cliente em kebab-case; nunca usar dados pessoais no alias. IdP do cliente: `<alias>-idp`.

### Clientes (tenants)
- Todo cliente é uma instância do módulo `tenant` — nunca `keycloak_organization` solto.
- Ordem de preferência para o login do cliente: **IdP OIDC** (Entra ID, Google Workspace, Okta) → **SAML** (ADFS) → **LDAP** → usuários locais.
- IdP do cliente fica **oculto** na tela de login; o usuário chega a ele digitando um e-mail do domínio do cliente.
- `keycloak_organization_memberships` só para tenants de usuários locais: ele é autoritativo e removeria quem entrou pelo IdP/LDAP.
- Segredos do cliente (client secret do IdP, senha de bind do LDAP) entram como variáveis sensíveis, nunca no código.
- Roles de realm padrão: `admin`, `gestor`, `usuario`. Roles específicas do sistema ficam no client.
- Recursos Terraform: `snake_case`; variáveis documentadas com `description`.

### Terraform
- Módulos pequenos e reutilizáveis; o realm golden é um módulo parametrizado, não cópia por sistema.
- Um state por ambiente; nada de `terraform apply` local em hml/prd — só via pipeline.
- `terraform fmt` e `terraform validate` obrigatórios antes de commit.
- Segredos nunca no código: usar variáveis sensíveis / cofre de segredos.

### Tema
- Construído com Keycloakify; **toda cor, fonte, espaço, raio e sombra vem de `design-system/tokens.css`**. Nada de hex solto.
- Regras de uso dos componentes estão em `design-system/README.md` (pill para ação, fill ≠ cor de texto, foco sempre visível).
- Mudou o HTML do design system? Atualize `tokens.css` no mesmo PR.
- **Nunca usar o nome "Loanza" nem o logo dela** (estudo de caso de terceiros). Nome, logo e favicon vêm só de `design-system/brand/`; trocar a identidade não pode exigir mudança de código.
- Mudança de layout = nova versão do JAR + nova tag da imagem Docker.

### Portal
- Campos mínimos: nome do sistema, endereço do front-end, roles extras, multi-tenant (sim/não). Cliente: alias, nome, domínios e forma de login.
- Toda ação de provisionamento é **idempotente** (repetir conserta o que faltar) e **auditada** (quem, quando, o quê, resultado).
- Provisiona pela **Admin API** (ADR 0004). `GoldenRealmTemplate` (Java) é cópia do módulo `realm-golden`: mudou um, mude o outro
  no mesmo PR — o teste ponta a ponta compara o realm criado pelo portal com o do Terraform e falha se divergirem.
- Segredos dos clientes (client secret de IdP, senha de LDAP) vão direto ao Keycloak e **nunca** são gravados nem auditados.
- Só quem tem a role `provisionador` no realm `plataforma` provisiona (regra no domínio: `Operator.assertCanProvision`).
- Keycloak 26.8+: a Admin API só aceita tokens com o escopo `kc-admin-api` — todo client que chama a Admin API precisa dele.

### Git
- Commits em português, no imperativo, com escopo: `feat(theme): ...`, `fix(terraform): ...`, `docs(adr): ...`.
- Uma mudança lógica por PR.

---

## 8. Segurança (inegociável)

- SPAs usam Authorization Code + **PKCE**; nunca Implicit nem Resource Owner Password.
- Tokens de acesso curtos; refresh tokens com rotação.
- Redirect URIs e Web Origins explícitas — **nunca `*`**.
- Nenhum segredo, chave ou `.env` real commitado. `.env.example` sim.
- Eventos de login e admin habilitados no realm golden.
- Isolamento de tenant é responsabilidade dupla: claim `organization` no token **e** filtro obrigatório na aplicação (os starters devem impor isso por padrão).

---

## 9. Qualidade e testes

- Ambiente local sobe com um comando (`docker compose up --build`) e já aplica o realm golden.
- `scripts/smoke-test.py` precisa passar antes de qualquer merge (o CI roda a cada push).
- Toda mudança no template deve ser testada aplicando-o num realm descartável.
- Testes mínimos: login com usuário local, login via IdP de uma Organization, login via LDAP de uma Organization, claim `organization` presente no token, roles corretas.
- Starters têm testes de validação de JWT, extração de tenant e checagem de roles.

---

## 10. Como trabalhar neste repo (para agentes)

- Leia este arquivo e os ADRs relevantes antes de propor mudanças.
- Siga a fase atual do roteiro; se a tarefa pedir algo de uma fase futura, avise antes de implementar.
- Ao tomar uma decisão de arquitetura, crie o ADR e atualize as seções 2–6 deste arquivo.
- Código novo segue Ports and Adapters (seção 6). Se não souber em que camada algo vai, provavelmente é um adapter.
- Prefira estender o template golden a criar configuração específica por sistema.
- Se algo só puder ser feito pelo console do Keycloak, pare e registre o motivo — provavelmente há um jeito via Terraform ou Admin API.
- Mantenha este arquivo curto e atual: remova o que ficou obsoleto em vez de acumular exceções.

---

## 11. Glossário

- **Realm golden**: template padrão de realm aplicado a todo sistema novo.
- **Organization**: tenant (cliente) dentro de um realm, com membros, domínios e IdP próprios.
- **Provisionamento**: criação automatizada de realm, clients, roles e organizations para um sistema.
- **Starter**: biblioteca interna que integra uma aplicação ao Keycloak com o mínimo de configuração.
- **Porta (port)**: interface que define o que a aplicação oferece (entrada) ou de que precisa (saída).
- **Adapter**: implementação concreta de uma porta (controller REST, cliente do Keycloak, repositório JPA).
