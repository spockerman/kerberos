# Um cliente (tenant) do sistema: Organization + forma de login do cliente (IdP próprio ou LDAP).

locals {
  idp_type    = var.identity_provider == null ? "" : var.identity_provider.type
  idp_alias   = "${var.alias}-idp"
  idp_display = var.identity_provider == null ? null : coalesce(var.identity_provider.display_name, var.name)
}

resource "keycloak_organization" "this" {
  realm   = var.realm_name
  name    = var.name
  alias   = var.alias
  enabled = true

  dynamic "domain" {
    for_each = var.domains
    content {
      name     = domain.value
      verified = true
    }
  }
}

# ---------- IdP do cliente: OIDC (Entra ID, Google Workspace, Okta, Keycloak...) ----------
resource "keycloak_oidc_identity_provider" "this" {
  count = local.idp_type == "oidc" ? 1 : 0

  realm        = var.realm_name
  alias        = local.idp_alias
  display_name = local.idp_display
  enabled      = true

  issuer             = var.identity_provider.issuer
  authorization_url  = var.identity_provider.authorization_url
  token_url          = var.identity_provider.token_url
  jwks_url           = var.identity_provider.jwks_url
  user_info_url      = var.identity_provider.user_info_url
  logout_url         = var.identity_provider.logout_url
  validate_signature = true
  client_id          = var.identity_provider.client_id
  client_secret      = var.identity_provider_client_secret
  default_scopes     = var.identity_provider.scopes
  sync_mode          = "FORCE" # nome/e-mail atualizados a cada login, vindos do cliente

  # O cliente responde pelos e-mails do próprio domínio; só aparece para quem digita esse domínio
  trust_email        = true
  hide_on_login_page = true
  store_token        = false

  organization_id                 = keycloak_organization.this.id
  org_domain                      = var.domains[0]
  org_redirect_mode_email_matches = var.identity_provider.redirect_on_email_domain
}

# ---------- IdP do cliente: SAML (ADFS, Entra ID SAML, Okta SAML...) ----------
resource "keycloak_saml_identity_provider" "this" {
  count = local.idp_type == "saml" ? 1 : 0

  realm        = var.realm_name
  alias        = local.idp_alias
  display_name = local.idp_display
  enabled      = true

  entity_id                  = var.identity_provider.entity_id
  single_sign_on_service_url = var.identity_provider.single_sign_on_service_url
  single_logout_service_url  = var.identity_provider.single_logout_service_url
  name_id_policy_format      = var.identity_provider.name_id_policy_format
  validate_signature         = var.identity_provider.signing_certificate != null
  signing_certificate        = var.identity_provider.signing_certificate
  want_assertions_signed     = true
  post_binding_response      = true
  post_binding_authn_request = true
  sync_mode                  = "FORCE"

  trust_email        = true
  hide_on_login_page = true
  store_token        = false

  organization_id                 = keycloak_organization.this.id
  org_domain                      = var.domains[0]
  org_redirect_mode_email_matches = var.identity_provider.redirect_on_email_domain
}

# ---------- LDAP/AD do cliente ----------
resource "keycloak_ldap_user_federation" "this" {
  count = var.ldap == null ? 0 : 1

  name     = "ldap-${var.alias}"
  realm_id = var.realm_id
  enabled  = true

  connection_url            = var.ldap.connection_url
  users_dn                  = var.ldap.users_dn
  bind_dn                   = var.ldap.bind_dn
  bind_credential           = var.ldap_bind_credential
  vendor                    = var.ldap.vendor
  username_ldap_attribute   = var.ldap.username_ldap_attribute
  rdn_ldap_attribute        = var.ldap.rdn_ldap_attribute
  uuid_ldap_attribute       = var.ldap.uuid_ldap_attribute
  user_object_classes       = var.ldap.user_object_classes
  custom_user_search_filter = var.ldap.custom_user_search_filter
  start_tls                 = var.ldap.start_tls

  # Somente leitura: senhas e dados continuam sendo do LDAP do cliente
  edit_mode          = "READ_ONLY"
  import_enabled     = true
  sync_registrations = false
  trust_email        = true
  connection_timeout = "5s"
  read_timeout       = "10s"
}

resource "keycloak_ldap_custom_mapper" "organization_membership" {
  count = var.ldap == null ? 0 : 1

  name                    = "organization-membership"
  realm_id                = var.realm_id
  ldap_user_federation_id = keycloak_ldap_user_federation.this[0].id
  provider_id             = "organization-membership-ldap-mapper"
  provider_type           = "org.keycloak.storage.ldap.mappers.LDAPStorageMapper"
  config = {
    "organization.alias" = keycloak_organization.this.alias
  }
}
