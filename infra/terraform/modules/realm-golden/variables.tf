variable "realm_name" {
  description = "Nome técnico do realm (kebab-case). Um realm por sistema/produto."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,40}$", var.realm_name))
    error_message = "Use kebab-case: letras minúsculas, números e hífen."
  }
}

variable "display_name" {
  description = "Nome exibido do sistema nas telas de login."
  type        = string
}

variable "login_theme" {
  description = "Tema de login aplicado ao realm (o JAR do Keycloakify registra o tema 'kerberos')."
  type        = string
  default     = "kerberos"
}

variable "ssl_required" {
  description = "Exigência de TLS: none, external (padrão; permite HTTP só em redes privadas) ou all."
  type        = string
  default     = "external"
}

variable "verify_email" {
  description = "Exige verificação de e-mail dos usuários."
  type        = bool
  default     = false
}

variable "realm_roles" {
  description = "Roles de realm padrão do template."
  type        = map(string)
  default = {
    admin   = "Administra o sistema e os usuários da organização"
    gestor  = "Gerencia dados e operações da organização"
    usuario = "Acesso básico ao sistema"
  }
}

variable "default_role" {
  description = "Role atribuída automaticamente a todo usuário novo."
  type        = string
  default     = "usuario"
}

variable "smtp" {
  description = "Servidor de e-mail do realm (esqueci a senha, verificação). null desativa o envio."
  type = object({
    host              = string
    port              = optional(number, 25)
    from              = string
    from_display_name = optional(string)
    starttls          = optional(bool, false)
    ssl               = optional(bool, false)
    username          = optional(string)
  })
  default = null
}

variable "smtp_password" {
  description = "Senha do SMTP (quando smtp.username é informado). Vem de variável de ambiente/cofre, nunca do código."
  type        = string
  default     = null
  sensitive   = true
}

variable "events_expiration_seconds" {
  description = "Tempo de retenção dos eventos de login e de administração."
  type        = number
  default     = 2592000 # 30 dias
}

variable "token_settings" {
  description = "Durações de token e sessão (strings de duração Go, ex.: 5m, 10h)."
  type = object({
    access_token_lifespan    = optional(string, "5m")
    sso_session_idle_timeout = optional(string, "30m")
    sso_session_max_lifespan = optional(string, "10h")
  })
  default = {}
}
