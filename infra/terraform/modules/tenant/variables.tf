variable "realm_id" {
  description = "ID do realm do sistema (saída realm_id do módulo realm-golden)."
  type        = string
}

variable "realm_name" {
  description = "Nome do realm do sistema (saída realm_name do módulo realm-golden)."
  type        = string
}

variable "alias" {
  description = "Alias estável do cliente em kebab-case. Vira o valor do claim `organization` no token. Não muda depois de criado."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9][a-z0-9-]{0,62}$", var.alias))
    error_message = "Use kebab-case: letras minúsculas, números e hífen."
  }
}

variable "name" {
  description = "Nome do cliente exibido nas telas."
  type        = string
}

variable "domains" {
  description = "Domínios de e-mail do cliente. O primeiro é usado para direcionar o login ao IdP do cliente."
  type        = list(string)

  validation {
    condition     = length(var.domains) > 0
    error_message = "Informe ao menos um domínio."
  }
}

variable "identity_provider" {
  description = <<-EOT
    Login pelo provedor de identidade do próprio cliente (Entra ID, Google Workspace, Okta, ADFS...).
    type = "oidc" ou "saml". Com redirect_on_email_domain, quem digita um e-mail do domínio do cliente
    é levado direto ao IdP dele. null = cliente usa usuários locais ou LDAP.
  EOT
  type = object({
    type                     = string
    display_name             = optional(string)
    redirect_on_email_domain = optional(bool, true)

    # OIDC
    issuer            = optional(string)
    authorization_url = optional(string)
    token_url         = optional(string)
    jwks_url          = optional(string)
    user_info_url     = optional(string)
    logout_url        = optional(string)
    client_id         = optional(string)
    scopes            = optional(string, "openid profile email")

    # SAML
    entity_id                  = optional(string)
    single_sign_on_service_url = optional(string)
    single_logout_service_url  = optional(string)
    signing_certificate        = optional(string)
    name_id_policy_format      = optional(string, "Email")
  })
  default = null

  validation {
    condition     = var.identity_provider == null ? true : contains(["oidc", "saml"], var.identity_provider.type)
    error_message = "identity_provider.type deve ser \"oidc\" ou \"saml\"."
  }
}

variable "identity_provider_client_secret" {
  description = "Segredo do client OIDC registrado no IdP do cliente. Vem de variável de ambiente/cofre."
  type        = string
  default     = null
  sensitive   = true
}

variable "ldap" {
  description = <<-EOT
    Federação com o LDAP/AD do cliente (somente leitura). Os usuários importados viram membros da
    organização pelo mapper `organization-membership-ldap-mapper` (provider próprio na imagem do Keycloak).
    Padrões para OpenLDAP; no Active Directory use vendor = "AD", username_ldap_attribute = "sAMAccountName",
    rdn_ldap_attribute = "cn", uuid_ldap_attribute = "objectGUID", user_object_classes = ["person", "organizationalPerson", "user"].
    null = sem LDAP.
  EOT
  type = object({
    connection_url            = string
    users_dn                  = string
    bind_dn                   = string
    vendor                    = optional(string, "OTHER")
    username_ldap_attribute   = optional(string, "uid")
    rdn_ldap_attribute        = optional(string, "uid")
    uuid_ldap_attribute       = optional(string, "entryUUID")
    user_object_classes       = optional(list(string), ["inetOrgPerson", "organizationalPerson"])
    custom_user_search_filter = optional(string)
    start_tls                 = optional(bool, false)
  })
  default = null
}

variable "ldap_bind_credential" {
  description = "Senha do usuário de leitura do LDAP do cliente. Vem de variável de ambiente/cofre."
  type        = string
  default     = null
  sensitive   = true
}
