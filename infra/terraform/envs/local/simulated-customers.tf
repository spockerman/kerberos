# Clientes SIMULADOS — só existem no ambiente local, para testar a federação ponta a ponta.
# Em produção, o IdP e o LDAP são do cliente; aqui ficam no mesmo Keycloak e num container OpenLDAP.

# "Initech" usa um IdP OIDC corporativo (o papel que Entra ID / Google Workspace / Okta fariam).
resource "keycloak_realm" "initech_idp" {
  realm                    = "initech-idp"
  enabled                  = true
  display_name             = "Initech SSO (simulado)"
  ssl_required             = "external"
  login_with_email_allowed = true
}

resource "keycloak_openid_client" "initech_broker" {
  realm_id      = keycloak_realm.initech_idp.id
  client_id     = "kerberos-broker"
  name          = "Kerberos (plataforma de identidade)"
  access_type   = "CONFIDENTIAL"
  client_secret = var.initech_client_secret

  standard_flow_enabled        = true
  direct_access_grants_enabled = false
  full_scope_allowed           = false
  valid_redirect_uris          = ["${local.keycloak_public_url}/realms/${module.realm.realm_name}/broker/initech-idp/endpoint"]
  valid_post_logout_redirect_uris = [
    "${local.keycloak_public_url}/realms/${module.realm.realm_name}/broker/initech-idp/endpoint/logout_response",
  ]
}

resource "keycloak_user" "initech_diego" {
  realm_id       = keycloak_realm.initech_idp.id
  username       = "diego"
  enabled        = true
  email          = "diego@initech.test"
  email_verified = true
  first_name     = "Diego"
  last_name      = "Alves"

  initial_password {
    value     = var.demo_user_password
    temporary = false
  }
}
