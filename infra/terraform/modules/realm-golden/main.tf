# Realm golden: o template aplicado a TODO sistema novo.
# Mudanças aqui valem para todos os realms na próxima aplicação do Terraform.

resource "keycloak_realm" "this" {
  realm        = var.realm_name
  enabled      = true
  display_name = var.display_name

  # Multi-tenancy: cada cliente (tenant) é uma Organization dentro deste realm.
  organizations_enabled = true

  # Layout da empresa em todo realm, sem passo manual.
  login_theme = var.login_theme

  ssl_required = var.ssl_required

  # Login
  registration_allowed     = false # B2B: usuários entram por convite, IdP da organização ou provisionamento
  reset_password_allowed   = true
  remember_me              = true
  login_with_email_allowed = true
  duplicate_emails_allowed = false
  verify_email             = var.verify_email

  password_policy = "length(12) and notUsername and notEmail and passwordHistory(3)"

  # Tokens curtos e refresh token com rotação (uso único)
  access_token_lifespan    = var.token_settings.access_token_lifespan
  sso_session_idle_timeout = var.token_settings.sso_session_idle_timeout
  sso_session_max_lifespan = var.token_settings.sso_session_max_lifespan
  revoke_refresh_token     = true
  refresh_token_max_reuse  = 0

  internationalization {
    supported_locales = ["pt-BR", "en"]
    default_locale    = "pt-BR"
  }

  security_defenses {
    headers {
      x_frame_options                     = "SAMEORIGIN"
      content_security_policy             = "frame-src 'self'; frame-ancestors 'self'; object-src 'none';"
      content_security_policy_report_only = ""
      x_content_type_options              = "nosniff"
      x_robots_tag                        = "none"
      x_xss_protection                    = "1; mode=block"
      strict_transport_security           = "max-age=31536000; includeSubDomains"
    }
    brute_force_detection {
      permanent_lockout                = false
      max_login_failures               = 10
      wait_increment_seconds           = 60
      quick_login_check_milli_seconds  = 1000
      minimum_quick_login_wait_seconds = 60
      max_failure_wait_seconds         = 900
      failure_reset_time_seconds       = 43200
    }
  }

  dynamic "smtp_server" {
    for_each = var.smtp == null ? [] : [var.smtp]
    content {
      host              = smtp_server.value.host
      port              = smtp_server.value.port
      from              = smtp_server.value.from
      from_display_name = smtp_server.value.from_display_name
      starttls          = smtp_server.value.starttls
      ssl               = smtp_server.value.ssl

      dynamic "auth" {
        for_each = smtp_server.value.username == null ? [] : [smtp_server.value]
        content {
          username = auth.value.username
          password = var.smtp_password
        }
      }
    }
  }
}

# Auditoria: eventos de login e de administração salvos no banco.
resource "keycloak_realm_events" "this" {
  realm_id                     = keycloak_realm.this.id
  events_enabled               = true
  events_expiration            = var.events_expiration_seconds
  admin_events_enabled         = true
  admin_events_details_enabled = true
  events_listeners             = ["jboss-logging"]
}

# Roles padrão de todo sistema. Roles específicas do negócio ficam no client do sistema.
resource "keycloak_role" "realm" {
  for_each    = var.realm_roles
  realm_id    = keycloak_realm.this.id
  name        = each.key
  description = each.value
}

resource "keycloak_default_roles" "this" {
  realm_id      = keycloak_realm.this.id
  default_roles = ["offline_access", "uma_authorization", keycloak_role.realm[var.default_role].name]
}

# Escopo "organization" criado pelo Keycloak em todo realm. Os clients do template o usam
# para colocar o claim `organization` (alias da organização) no token.
data "keycloak_openid_client_scope" "organization" {
  realm_id   = keycloak_realm.this.id
  name       = "organization"
  depends_on = [keycloak_realm.this]
}
