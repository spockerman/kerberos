# 0003 — LDAP por organização com um mapper próprio

- Status: aceita (2026-11-24)

## Contexto
Cada cliente (tenant) é uma Organization. Para IdPs (OIDC/SAML), o Keycloak 26 liga o IdP à Organization
nativamente: quem entra pelo IdP do cliente vira membro e recebe o claim `organization`.
Para LDAP não existe esse vínculo: a federação LDAP é do realm inteiro e os usuários importados não entram
em nenhuma Organization — o token sairia sem tenant.

## Decisão
Criar um provider próprio, `organization-membership-ldap-mapper` (`keycloak/providers/organization-ldap-mapper`),
um mapper LDAP que adiciona cada usuário importado à Organization configurada. O módulo Terraform `tenant`
cria a federação LDAP do cliente (somente leitura) já com esse mapper.

Preferência continua sendo o IdP do cliente (Entra ID, Google Workspace, ADFS via SAML): LDAP direto exige
rede até o diretório do cliente (VPN/túnel) e expõe uma credencial de leitura.

## Consequências
- O JAR entra na imagem do Keycloak e é compilado contra a mesma versão (`KEYCLOAK_VERSION`).
  A cada atualização do Keycloak, o teste ponta a ponta (usuária `eva`) confirma que o mapper continua funcionando.
- É uma extensão do Keycloak, não código de aplicação: a regra de Ports and Adapters (seção 6 do CLAUDE.md) não se aplica.
- Se uma versão futura do Keycloak trouxer o vínculo LDAP ↔ Organization nativo, este provider deve ser removido.
