terraform {
  required_version = ">= 1.6"
  required_providers {
    keycloak = {
      source  = "keycloak/keycloak"
      version = "~> 5.11"
    }
  }

  # Ambiente local: state em arquivo, guardado no volume Docker "terraform-state".
  # hml/prd usam backend remoto (S3) — nunca state local.
  backend "local" {}
}

# Credenciais via variáveis de ambiente (KEYCLOAK_URL, KEYCLOAK_CLIENT_ID, KEYCLOAK_USER, KEYCLOAK_PASSWORD),
# definidas no docker-compose. Nada de segredo neste arquivo.
provider "keycloak" {}
