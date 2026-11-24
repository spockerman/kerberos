package br.kerberos.demo.me.adapter.out.security;

import br.kerberos.demo.me.application.port.out.CurrentUserPort;
import br.kerberos.demo.me.domain.CurrentUser;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Lê o usuário autenticado do contexto do Spring Security (token JWT validado). */
public class JwtCurrentUserAdapter implements CurrentUserPort {

    private final KeycloakClaimsMapper mapper = new KeycloakClaimsMapper();

    @Override
    public Optional<CurrentUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        return Optional.of(mapper.toCurrentUser(jwt));
    }
}
