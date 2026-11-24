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
