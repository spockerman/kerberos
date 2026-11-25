package br.kerberos.portal.provisioning.domain;

import java.util.Objects;

/** Pessoa do time interno usando o portal. */
public record Operator(String username, boolean canProvision) {

    public Operator {
        Objects.requireNonNull(username, "operador sem identificação");
    }

    public void assertCanProvision() {
        if (!canProvision) {
            throw new NotAllowedException(username + " não tem a role de provisionamento");
        }
    }
}
