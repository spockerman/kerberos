variable "demo_web_url" {
  description = "URL do front-end de exemplo."
  type        = string
  default     = "http://localhost:3000"
}

variable "demo_user_password" {
  description = "Senha dos usuários de teste. SOMENTE ambiente local."
  type        = string
  default     = "Senha@Demo2026!"
  sensitive   = true
}

variable "smtp_host" {
  description = "Servidor SMTP local (Mailpit no docker-compose)."
  type        = string
  default     = "mailpit"
}

variable "initech_client_secret" {
  description = "Segredo do client do Kerberos no IdP simulado da Initech. SOMENTE ambiente local."
  type        = string
  default     = "segredo-local-initech"
  sensitive   = true
}

variable "umbrella_ldap_password" {
  description = "Senha do admin do LDAP simulado da Umbrella (container ldap-umbrella). SOMENTE ambiente local."
  type        = string
  default     = "admin-umbrella"
  sensitive   = true
}

variable "portal_web_url" {
  description = "URL do front-end do portal de provisionamento."
  type        = string
  default     = "http://localhost:3001"
}

variable "portal_provisioner_secret" {
  description = "Segredo da conta de serviço do portal no realm master. SOMENTE ambiente local."
  type        = string
  default     = "segredo-local-portal"
  sensitive   = true
}
