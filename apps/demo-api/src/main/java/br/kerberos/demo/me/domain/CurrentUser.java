package br.kerberos.demo.me.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Usuário autenticado, como o domínio o enxerga. Não conhece JWT, Spring nem Keycloak:
 * o adapter de segurança converte o token neste tipo.
 */
public record CurrentUser(String userId, String username, String email, List<TenantId> tenants, Set<String> roles) {

    public CurrentUser {
        Objects.requireNonNull(userId, "userId obrigatório");
        tenants = List.copyOf(tenants);
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean belongsTo(TenantId tenant) {
        return tenants.contains(tenant);
    }

    /** Regra de isolamento: só acessa dados de um tenant quem é membro dele. */
    public void assertCanAccess(TenantId tenant) {
        if (!belongsTo(tenant)) {
            throw new TenantAccessDeniedException(tenant);
        }
    }
}
