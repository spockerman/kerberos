# Imagem corporativa do Keycloak: versão fixada + tema da plataforma, já otimizada (kc.sh build).
# Versão do Keycloak: definida SOMENTE aqui.
ARG KEYCLOAK_VERSION=26.8.2

# 1) Tema (Keycloakify) → JAR
FROM node:22-bookworm AS theme
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven openjdk-17-jdk-headless \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /repo
COPY design-system ./design-system
COPY theme/package.json theme/package-lock.json ./theme/
RUN cd theme && npm ci --no-audit --no-fund
COPY theme ./theme
RUN cd theme && npm run build-keycloak-theme

# 2) Keycloak com o tema e as opções de build
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} AS builder
ENV KC_DB=postgres \
    KC_HEALTH_ENABLED=true \
    KC_METRICS_ENABLED=true
COPY --from=theme /repo/theme/dist_keycloak/kerberos-theme.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

# 3) Imagem final
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}
COPY --from=builder /opt/keycloak/ /opt/keycloak/
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
