# Client de uma SPA (Next.js/React): público, Authorization Code + PKCE, sem segredo no navegador.
resource "keycloak_openid_client" "web" {
  realm_id    = var.realm_id
  client_id   = "${var.app_name}-web"
  name        = var.display_name
  access_type = "PUBLIC"

  standard_flow_enabled        = true
  implicit_flow_enabled        = false
  direct_access_grants_enabled = false
  service_accounts_enabled     = false
  pkce_code_challenge_method   = "S256"

  root_url                        = var.base_url
  base_url                        = var.base_url
  valid_redirect_uris             = [for p in var.redirect_paths : "${var.base_url}${p}"]
  valid_post_logout_redirect_uris = ["${var.base_url}/*"]
  web_origins                     = [var.base_url]
}

# Escopos sempre presentes no token. "organization" traz o tenant do usuário.
# Recurso autoritativo: escopos fora destas listas são removidos do client.
resource "keycloak_client_scope_assignments" "web" {
  realm_id        = var.realm_id
  client_id       = keycloak_openid_client.web.id
  optional_scopes = ["offline_access"]
  default_scopes = [
    "basic",
    "profile",
    "email",
    "roles",
    "web-origins",
    "acr",
    var.organization_scope_name,
  ]
}

# A API valida o audience: o token emitido para a SPA é aceito só pela API deste sistema.
resource "keycloak_openid_audience_protocol_mapper" "api_audience" {
  realm_id                 = var.realm_id
  client_id                = keycloak_openid_client.web.id
  name                     = "audience-${var.api_audience}"
  included_custom_audience = var.api_audience
  add_to_id_token          = false
  add_to_access_token      = true
}
