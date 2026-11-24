output "client_id" {
  description = "client_id público da SPA (vai no .env do front-end)."
  value       = keycloak_openid_client.web.client_id
}
