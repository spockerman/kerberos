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

# ---------- Tenants de exemplo ----------
locals {
  organizations = {
    acme   = { name = "Acme", domain = "acme.test" }
    globex = { name = "Globex", domain = "globex.test" }
  }

  # username => dados, organizações e roles. Carla está em duas organizações (testa a escolha de tenant).
  users = {
    ana   = { first = "Ana", last = "Souza", email = "ana@acme.test", orgs = ["acme"], roles = ["admin"] }
    bruno = { first = "Bruno", last = "Lima", email = "bruno@globex.test", orgs = ["globex"], roles = [] }
    carla = { first = "Carla", last = "Mendes", email = "carla@acme.test", orgs = ["acme", "globex"], roles = ["gestor"] }
  }
}

resource "keycloak_organization" "this" {
  for_each = local.organizations

  realm   = module.realm.realm_name
  name    = each.value.name
  alias   = each.key
  enabled = true

  domain {
    name     = each.value.domain
    verified = true
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

resource "keycloak_organization_memberships" "this" {
  for_each = local.organizations

  realm_id        = module.realm.realm_id
  organization_id = keycloak_organization.this[each.key].id
  members         = [for username, u in local.users : keycloak_user.this[username].username if contains(u.orgs, each.key)]
}
