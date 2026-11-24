package br.kerberos.demo.me.adapter.out.security;

import br.kerberos.demo.me.domain.CurrentUser;
import br.kerberos.demo.me.domain.TenantId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Traduz os claims do Keycloak para o tipo de domínio {@link CurrentUser}.
 * É o único lugar que conhece o formato do token.
 *
 * <ul>
 *   <li>{@code organization}: lista de aliases (padrão do Keycloak) ou objeto alias → atributos (formato JSON)</li>
 *   <li>{@code realm_access.roles}: roles de realm</li>
 * </ul>
 */
public class KeycloakClaimsMapper {

    static final String ORGANIZATION_CLAIM = "organization";
    static final String REALM_ACCESS_CLAIM = "realm_access";

    public CurrentUser toCurrentUser(Jwt jwt) {
        return new CurrentUser(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                tenants(jwt),
                realmRoles(jwt));
    }

    private List<TenantId> tenants(Jwt jwt) {
        Object claim = jwt.getClaims().get(ORGANIZATION_CLAIM);
        Collection<?> aliases;
        if (claim instanceof Map<?, ?> map) {
            aliases = map.keySet();
        } else if (claim instanceof Collection<?> list) {
            aliases = list;
        } else if (claim instanceof String single) {
            aliases = List.of(single);
        } else {
            aliases = List.of();
        }
        return aliases.stream().map(String::valueOf).map(TenantId::new).distinct().toList();
    }

    private Set<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return Set.of();
        }
        return roles.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
    }
}
