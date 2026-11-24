# Ambiente local: um sistema de exemplo ("demo") criado a partir do realm golden,
# com duas organizações (tenants) e usuários de teste.

module "realm" {
  source = "../../modules/realm-golden"

  realm_name   = "demo"
  display_name = "Demo"
  smtp = {
    host              = var.smtp_host
    port              = 1025
    from              = "nao-responda@demo.local"
    from_display_name = "Demo"
  }
}

module "demo_app" {
  source = "../../modules/spa-app"

  realm_id                = module.realm.realm_id
  app_name                = "demo"
  display_name            = "Demo"
  base_url                = var.demo_web_url
  api_audience            = "demo-api"
  organization_scope_name = module.realm.organization_scope_name
}

# ---------- Tenants (clientes) de exemplo ----------
# acme e globex: usuários locais do Keycloak.
# initech: login pelo IdP do próprio cliente (OIDC) — simulado pelo realm initech-idp (simulated-customers.tf).
# umbrella: login pelo LDAP do próprio cliente — simulado pelo container ldap-umbrella.
module "tenant" {
  source = "../../modules/tenant"

  for_each = {
    acme   = { name = "Acme", domains = ["acme.test"] }
    globex = { name = "Globex", domains = ["globex.test"] }
  }

  realm_id   = module.realm.realm_id
  realm_name = module.realm.realm_name
  alias      = each.key
  name       = each.value.name
  domains    = each.value.domains
}

module "tenant_initech" {
  source = "../../modules/tenant"

  realm_id   = module.realm.realm_id
  realm_name = module.realm.realm_name
  alias      = "initech"
  name       = "Initech"
  domains    = ["initech.test"]

  identity_provider = {
    type              = "oidc"
    display_name      = "Initech (SSO corporativo)"
    issuer            = "${local.keycloak_public_url}/realms/initech-idp"
    authorization_url = "${local.keycloak_public_url}/realms/initech-idp/protocol/openid-connect/auth"
    token_url         = "${local.keycloak_public_url}/realms/initech-idp/protocol/openid-connect/token"
    jwks_url          = "${local.keycloak_public_url}/realms/initech-idp/protocol/openid-connect/certs"
    user_info_url     = "${local.keycloak_public_url}/realms/initech-idp/protocol/openid-connect/userinfo"
    logout_url        = "${local.keycloak_public_url}/realms/initech-idp/protocol/openid-connect/logout"
    client_id         = keycloak_openid_client.initech_broker.client_id
  }
  identity_provider_client_secret = var.initech_client_secret
}

module "tenant_umbrella" {
  source = "../../modules/tenant"

  realm_id   = module.realm.realm_id
  realm_name = module.realm.realm_name
  alias      = "umbrella"
  name       = "Umbrella"
  domains    = ["umbrella.test"]

  ldap = {
    connection_url = "ldap://ldap-umbrella:389"
    users_dn       = "ou=people,dc=umbrella,dc=test"
    bind_dn        = "cn=admin,dc=umbrella,dc=test"
  }
  ldap_bind_credential = var.umbrella_ldap_password
}

# Antes da Fase 2 as organizações eram recursos soltos; agora vêm do módulo tenant.
moved {
  from = keycloak_organization.this["acme"]
  to   = module.tenant["acme"].keycloak_organization.this
}

moved {
  from = keycloak_organization.this["globex"]
  to   = module.tenant["globex"].keycloak_organization.this
}

# ---------- Usuários locais de teste ----------
locals {
  keycloak_public_url = "http://localhost:8080"

  # username => dados, organizações e roles. Carla está em duas organizações (testa a escolha de tenant).
  users = {
    ana   = { first = "Ana", last = "Souza", email = "ana@acme.test", orgs = ["acme"], roles = ["admin"] }
    bruno = { first = "Bruno", last = "Lima", email = "bruno@globex.test", orgs = ["globex"], roles = [] }
    carla = { first = "Carla", last = "Mendes", email = "carla@acme.test", orgs = ["acme", "globex"], roles = ["gestor"] }
  }
}

resource "keycloak_user" "this" {
  for_each = local.users

  realm_id       = module.realm.realm_id
  username       = each.key
  enabled        = true
  email          = each.value.email
  email_verified = true
  first_name     = each.value.first
  last_name      = each.value.last

  initial_password {
    value     = var.demo_user_password
    temporary = false
  }
}

resource "keycloak_user_roles" "this" {
  for_each = { for k, u in local.users : k => u if length(u.roles) > 0 }

  realm_id   = module.realm.realm_id
  user_id    = keycloak_user.this[each.key].id
  exhaustive = false
  role_ids   = [for r in each.value.roles : module.realm.role_ids[r]]
}

# Só para tenants de usuários locais. Em initech/umbrella os membros entram pelo IdP/LDAP do cliente;
# este recurso é autoritativo e removeria esses membros.
resource "keycloak_organization_memberships" "this" {
  for_each = module.tenant

  realm_id        = module.realm.realm_id
  organization_id = each.value.organization_id
  members         = [for username, u in local.users : keycloak_user.this[username].username if contains(u.orgs, each.key)]
}
