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
| Design system | Tokens visuais (`tokens.css`), fonte Outfit e regras de uso | `design-system/` |
| Tema (Keycloakify) | Login, conta e e-mail no layout da empresa; tema **padrão do servidor** | `theme/` |
| Realm golden | Template de realm em Terraform (fluxos, MFA, senha, roles, scopes, SMTP, eventos) | `infra/terraform/modules/realm-golden/` |
| Ambientes | Composição dos módulos por ambiente (dev, hml, prd) | `infra/terraform/envs/` |
| Portal de Provisionamento | Formulário web que cria realm/clients/organizations e gera o `.env` | `portal/` |
| Starters/SDKs | Bibliotecas que validam JWT, extraem tenant e checam roles | `starters/` |
| Ambiente local | docker-compose com Keycloak + Postgres (+ Mailpit) | `local/` |
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
5. **Configuração como código**: Terraform (provider oficial `keycloak/keycloak`) é a fonte da verdade.
   Exports JSON do console servem só como referência, nunca como mecanismo de deploy.
6. **Tema como padrão do servidor**: todo realm nasce com o layout da empresa, sem passo extra.
7. **Portal usa service account** com o mínimo de permissões (`realm-management` necessárias), nunca
   o usuário admin do realm `master`.
8. **Arquitetura hexagonal (Ports and Adapters)** em todo código de aplicação (portal e starters). Ver seção 6.
9. **Produção na AWS com custo mínimo** enquanto não houver exigência de alta disponibilidade. Ver seção 5.

Mudar qualquer item acima exige um ADR em `docs/adr/NNNN-titulo.md` (contexto, decisão, consequências).

---

## 4. Roteiro e estado atual

| Fase | Entrega | Status |
|---|---|---|
| 1 | Imagem Docker + tema + realm golden + ambiente local | ⬜ não iniciada |
| 2 | Organizations + federação por cliente no template | ⬜ |
| 3 | Portal de provisionamento + geração do `.env` | ⬜ |
| 4 | Starters e SDKs das stacks | ⬜ |

Atualize esta tabela ao concluir cada fase. Não adiante fases: construa sobre o que já está estável.

---

## 5. Stacks e infraestrutura

| Item | Decisão |
|---|---|
| Keycloak | **26.8.2** — versão fixada num único lugar (`keycloak/VERSION` ou `.env`) e referenciada no repo todo |
| Back-end (portal e sistemas) | **Java + Spring Boot** |
| Front-end (portal e sistemas) | **React / Next.js** |
| Tema de login | **Keycloakify** (React), usando os tokens de `design-system/tokens.css` |
| Design system | `Loanza Design System.html` (raiz) → extraído para `design-system/`. Tema claro apenas, fonte Outfit. **Só referência de estilo** |
| Identidade (nome, logo) | **Própria, a definir** — placeholders em `design-system/brand/brand.json` |
| Configuração do Keycloak | **Terraform** (provider `keycloak/keycloak`) |
| Banco | **PostgreSQL** |
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
<base>.<feature>/
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
- Organization: alias estável do cliente em kebab-case; nunca usar dados pessoais no alias.
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
- Campos mínimos: nome do sistema, URLs de redirect/origens, tipo de app, roles iniciais, multi-tenant (sim/não).
- Toda ação de provisionamento é **idempotente** e **auditada** (quem, quando, o quê).
- Preferir disparar o pipeline Terraform a chamar a Admin API diretamente, para manter rastreabilidade.

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

- Ambiente local sobe com um comando (`docker compose up`) e já aplica o realm golden.
- Toda mudança no template deve ser testada aplicando-o num realm descartável.
- Testes mínimos: login com usuário local, login via IdP de uma Organization, claim `organization` presente no token, roles corretas.
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
