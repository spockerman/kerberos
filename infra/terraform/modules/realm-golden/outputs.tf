output "realm_id" {
  description = "ID do realm, usado pelos demais módulos."
  value       = keycloak_realm.this.id
}

output "realm_name" {
  description = "Nome técnico do realm."
  value       = keycloak_realm.this.realm
}

output "role_ids" {
  description = "IDs das roles de realm padrão, por nome."
  value       = { for name, role in keycloak_role.realm : name => role.id }
}

output "organization_scope_name" {
  description = "Nome do client scope que emite o claim `organization`."
  value       = data.keycloak_openid_client_scope.organization.name
}
