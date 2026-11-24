package br.kerberos.demo.me.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.kerberos.demo.me.domain.CurrentUser;
import br.kerberos.demo.me.domain.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakClaimsMapperTest {

    private final KeycloakClaimsMapper mapper = new KeycloakClaimsMapper();

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("u-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("preferred_username", "carla")
                .claim("email", "carla@acme.test")
                .claim("realm_access", Map.of("roles", List.of("gestor", "usuario")));
    }

    @Test
    void organizacaoComoListaDeAliases() {
        CurrentUser user = mapper.toCurrentUser(token().claim("organization", List.of("acme", "globex")).build());

        assertThat(user.tenants()).containsExactly(new TenantId("acme"), new TenantId("globex"));
        assertThat(user.roles()).containsExactlyInAnyOrder("gestor", "usuario");
        assertThat(user.username()).isEqualTo("carla");
    }

    @Test
    void organizacaoNoFormatoJson() {
        CurrentUser user = mapper.toCurrentUser(
                token().claim("organization", Map.of("acme", Map.of("id", "123"))).build());

        assertThat(user.tenants()).containsExactly(new TenantId("acme"));
    }

    @Test
    void semOrganizacaoNemRoles() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("u-2").build();

        CurrentUser user = mapper.toCurrentUser(jwt);

        assertThat(user.tenants()).isEmpty();
        assertThat(user.roles()).isEmpty();
    }
}
