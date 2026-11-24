output "realm" {
  description = "Realm criado."
  value       = module.realm.realm_name
}

output "demo_web_client_id" {
  description = "client_id do front-end de exemplo."
  value       = module.demo_app.client_id
}
