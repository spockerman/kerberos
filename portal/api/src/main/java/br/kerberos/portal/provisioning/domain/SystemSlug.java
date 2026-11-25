package br.kerberos.portal.provisioning.domain;

import java.util.Objects;
import java.util.Set;

/** Nome técnico do sistema: vira o nome do realm e o prefixo dos clients (ex.: financeiro → financeiro-web). */
public record SystemSlug(String value) {

    /** Realms que já existem na plataforma e não podem ser criados pelo portal. */
    private static final Set<String> RESERVED = Set.of("master", "plataforma", "admin", "realms", "resources");

    public SystemSlug {
        Objects.requireNonNull(value, "nome do sistema obrigatório");
        if (!value.matches("[a-z][a-z0-9-]{1,40}") || value.endsWith("-") || value.contains("--")) {
            throw new InvalidSpecException("Nome do sistema inválido: use kebab-case, 2 a 41 caracteres (ex.: financeiro)");
        }
        if (RESERVED.contains(value) || value.endsWith("-idp")) {
            throw new InvalidSpecException("Nome reservado pela plataforma: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
