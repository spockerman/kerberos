package br.kerberos.portal.provisioning.adapter.out.security;

import br.kerberos.portal.provisioning.application.port.out.CurrentOperatorPort;
import br.kerberos.portal.provisioning.domain.NotAllowedException;
import br.kerberos.portal.provisioning.domain.Operator;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Operador a partir do token do realm da plataforma: usuário e se tem a role de provisionamento. */
public class JwtCurrentOperatorAdapter implements CurrentOperatorPort {

    private final String requiredRole;

    public JwtCurrentOperatorAdapter(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public Operator current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new NotAllowedException("Operador não autenticado");
        }
        String username = jwt.getClaimAsString("preferred_username");
        return new Operator(username == null ? jwt.getSubject() : username, hasRole(jwt));
    }

    private boolean hasRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        return realmAccess != null
                && realmAccess.get("roles") instanceof Collection<?> roles
                && roles.contains(requiredRole);
    }
}
