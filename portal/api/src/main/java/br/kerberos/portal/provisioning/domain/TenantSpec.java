package br.kerberos.portal.provisioning.domain;

import java.util.List;
import java.util.Objects;

/** Um cliente (tenant) de um sistema: vira uma Organization no realm do sistema. */
public record TenantSpec(TenantAlias alias, String name, List<String> domains, LoginMethod loginMethod) {

    public TenantSpec {
        Objects.requireNonNull(alias, "alias do cliente obrigatório");
        if (name == null || name.isBlank()) {
            throw new InvalidSpecException("Nome do cliente obrigatório");
        }
        name = name.strip();
        domains = domains == null ? List.of() : domains.stream().map(d -> d.strip().toLowerCase()).filter(d -> !d.isEmpty()).distinct().toList();
        if (domains.isEmpty()) {
            throw new InvalidSpecException("Informe ao menos um domínio de e-mail do cliente");
        }
        for (String domain : domains) {
            if (!domain.matches("[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+")) {
                throw new InvalidSpecException("Domínio inválido: " + domain);
            }
        }
        loginMethod = loginMethod == null ? new LoginMethod.LocalUsers() : loginMethod;
    }

    /** Domínio que leva o usuário ao IdP do cliente (o primeiro informado). */
    public String primaryDomain() {
        return domains.getFirst();
    }

    /** Regra da plataforma: cliente só existe em sistema multi-tenant. */
    public static void assertAllowedIn(SystemSpec system) {
        if (!system.multiTenant()) {
            throw new InvalidSpecException("O sistema " + system.slug() + " não é multi-tenant: não recebe clientes");
        }
    }
}
