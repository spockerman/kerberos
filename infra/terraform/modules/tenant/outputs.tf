output "organization_id" {
  description = "ID da Organization (para memberships e vínculos)."
  value       = keycloak_organization.this.id
}

output "alias" {
  description = "Alias do tenant, igual ao valor do claim `organization` no token."
  value       = keycloak_organization.this.alias
}

output "identity_provider_alias" {
  description = "Alias do IdP do cliente no Keycloak (null se não houver). A URL de retorno a cadastrar no IdP do cliente é <keycloak>/realms/<realm>/broker/<alias>/endpoint."
  value       = local.idp_type == "" ? null : local.idp_alias
}
