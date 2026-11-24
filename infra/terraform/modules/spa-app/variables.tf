variable "realm_id" {
  description = "Realm onde o sistema vive (saída realm_id do módulo realm-golden)."
  type        = string
}

variable "app_name" {
  description = "Nome do sistema em kebab-case. Gera o client <app_name>-web."
  type        = string
}

variable "display_name" {
  description = "Nome do client exibido no console e na tela de consentimento."
  type        = string
}

variable "base_url" {
  description = "URL pública do front-end (ex.: https://app.empresa.com ou http://localhost:3000)."
  type        = string
}

variable "redirect_paths" {
  description = "Caminhos de retorno após login, relativos a base_url. Nunca use curinga solto."
  type        = list(string)
  default     = ["/*"]
}

variable "api_audience" {
  description = "Audience que a API do sistema exige no token (ex.: demo-api)."
  type        = string
}

variable "organization_scope_name" {
  description = "Client scope que emite o claim organization (saída do módulo realm-golden)."
  type        = string
}
