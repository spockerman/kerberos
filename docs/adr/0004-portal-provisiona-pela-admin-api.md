# 0004 — Portal provisiona pela Admin API do Keycloak

- Status: aceita (2026-11-25) — ajusta a decisão 5 e a convenção "Portal" do CLAUDE.md

## Contexto
A Fase 3 cria o portal onde o time cadastra sistemas e clientes com poucos campos. A convenção anterior
preferia que o portal disparasse o pipeline Terraform. Na prática isso exige um repositório/pipeline com
acesso ao Keycloak de cada ambiente e faz cada cadastro esperar uma execução de pipeline.

## Decisão
O portal aplica as mudanças **direto pela Admin REST API** do Keycloak.

- **Terraform continua sendo a fonte da verdade da base da plataforma**: realm golden (módulo), realm `plataforma`,
  conta de serviço do portal, ambiente local e clientes simulados.
- **Sistemas e clientes criados pelo portal** são de responsabilidade do portal. O catálogo e a trilha de
  auditoria ficam no banco do portal (`portal-db`). Esses realms não são geridos pelo Terraform.
- O template golden é **reproduzido em Java** no adapter `adapter.out.keycloak` do portal. Para evitar que as
  duas cópias divirjam, o teste ponta a ponta compara as configurações do realm criado pelo portal com as do
  realm `demo`, criado pelo Terraform (tema, senha, força bruta, tokens, Organizations, eventos, roles).
- Toda operação é **idempotente** (rodar de novo conserta o que faltar) e **auditada** (quem, quando, o quê, resultado).
  Segredos dos clientes (client secret de IdP, senha de LDAP) vão direto ao Keycloak e **nunca** são gravados no portal.
- Permissões: a conta de serviço `portal-provisioner` tem só `create-realm` no realm `master`. O Keycloak
  concede a ela a administração de cada realm que ela cria, e de nenhum outro.

## Consequências
- Cadastro imediato, sem depender de pipeline.
- Mudança no módulo `realm-golden` precisa ser repetida em `GoldenRealmTemplate` (o teste de divergência falha até isso acontecer).
- Realms criados pelo portal não aparecem no `terraform plan`. Se um dia quisermos trazê-los para o Terraform,
  o catálogo do portal tem os dados para gerar os blocos.
