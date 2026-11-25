package br.kerberos.portal.provisioning.domain;

import java.util.Objects;

/** Alias estável do cliente: vira o valor do claim {@code organization} no token. */
public record TenantAlias(String value) {

    public TenantAlias {
        Objects.requireNonNull(value, "alias do cliente obrigatório");
        if (!value.matches("[a-z0-9][a-z0-9-]{0,62}") || value.endsWith("-")) {
            throw new InvalidSpecException("Alias do cliente inválido: use kebab-case (ex.: prefeitura-osasco)");
        }
    }

    public String identityProviderAlias() {
        return value + "-idp";
    }

    @Override
    public String toString() {
        return value;
    }
}
