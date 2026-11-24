# 0001 — Docker como único pré-requisito

- Status: aceita (2026-11-24)

## Contexto
O ambiente precisa de Keycloak, Postgres, Terraform, Node/Maven (tema), Java (API) e Node (front).
Exigir cada ferramenta instalada na máquina de cada pessoa gera diferenças de versão e atrito para começar.

## Decisão
Tudo roda em containers. Terraform é o serviço `terraform` do `docker-compose.yml` (imagem oficial,
versão fixada) e aplica o ambiente sozinho quando o Keycloak fica pronto. Tema, API e front são compilados
dentro dos Dockerfiles.

## Consequências
- `git clone` + `docker compose up --build` é o caminho inteiro para rodar.
- O mesmo arranjo serve ao CI e ao servidor (Lightsail).
- Para editar com recarga rápida (ex.: `npm run dev` no tema), a pessoa ainda pode instalar Node localmente — é opcional.
