# Plataforma: o realm do próprio portal de provisionamento (Central 156 — Plataforma de Identidade).
# Quem entra aqui é o time interno; só a role "provisionador" cria sistemas e clientes.

module "platform_realm" {
  source = "../../modules/realm-golden"

  realm_name   = "plataforma"
  display_name = "Plataforma de Identidade"
}

resource "keycloak_role" "provisioner" {
  realm_id    = module.platform_realm.realm_id
  name        = "provisionador"
  description = "Cria sistemas e cadastra clientes pelo portal de provisionamento"
}

module "portal_app" {
  source = "../../modules/spa-app"

  realm_id                = module.platform_realm.realm_id
  app_name                = "portal"
  display_name            = "Portal de Provisionamento"
  base_url                = var.portal_web_url
  api_audience            = "portal-api"
  organization_scope_name = module.platform_realm.organization_scope_name
}

# Usuários de teste do portal: operador (pode provisionar) e visitante (só entra, não provisiona)
locals {
  platform_users = {
    operador  = { first = "Olga", last = "Operadora", email = "operador@plataforma.local", roles = ["provisionador"] }
    visitante = { first = "Vitor", last = "Visitante", email = "visitante@plataforma.local", roles = [] }
  }
}

resource "keycloak_user" "platform" {
  for_each = local.platform_users

  realm_id       = module.platform_realm.realm_id
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

resource "keycloak_user_roles" "platform" {
  for_each = { for k, u in local.platform_users : k => u if length(u.roles) > 0 }

  realm_id   = module.platform_realm.realm_id
  user_id    = keycloak_user.platform[each.key].id
  exhaustive = false
  role_ids   = [keycloak_role.provisioner.id]
}

# Conta de serviço do portal no realm master. Só pode CRIAR realms; o Keycloak dá a ela a
# administração de cada realm que ela mesma cria — e nada sobre os demais (demo, plataforma...).
resource "keycloak_openid_client" "portal_provisioner" {
  realm_id      = "master"
  client_id     = "portal-provisioner"
  name          = "Portal de Provisionamento (conta de serviço)"
  access_type   = "CONFIDENTIAL"
  client_secret = var.portal_provisioner_secret

  service_accounts_enabled     = true
  standard_flow_enabled        = false
  direct_access_grants_enabled = false
}

# Desde o Keycloak 26.8 a Admin API só aceita tokens emitidos para ela: o escopo kc-admin-api
# coloca o audience certo no token (o admin-cli já vem com ele).
resource "keycloak_client_scope_assignments" "portal_provisioner" {
  realm_id        = "master"
  client_id       = keycloak_openid_client.portal_provisioner.id
  default_scopes  = ["basic", "roles", "kc-admin-api"]
  optional_scopes = []
}

resource "keycloak_openid_client_service_account_realm_role" "portal_create_realm" {
  realm_id                = "master"
  service_account_user_id = keycloak_openid_client.portal_provisioner.service_account_user_id
  role                    = "create-realm"
}
