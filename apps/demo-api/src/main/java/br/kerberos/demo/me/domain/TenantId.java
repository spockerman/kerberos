package br.kerberos.demo.me.domain;

import java.util.Objects;

/** Identificador do tenant (alias da Organization no Keycloak). */
public record TenantId(String value) {

    public TenantId {
        Objects.requireNonNull(value, "tenant obrigatório");
        if (!value.matches("[a-z0-9][a-z0-9-]{0,62}")) {
            throw new IllegalArgumentException("tenant inválido: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
