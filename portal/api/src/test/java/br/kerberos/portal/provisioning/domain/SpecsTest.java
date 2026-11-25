package br.kerberos.portal.provisioning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpecsTest {

    static SystemSpec financeiro(boolean multiTenant) {
        return new SystemSpec(new SystemSlug("financeiro"), "Financeiro", URI.create("https://financeiro.empresa.com"), List.of("auditor"), multiTenant);
    }

    @Test
    void sistemaDerivaClientEAudience() {
        SystemSpec s = financeiro(true);
        assertThat(s.webClientId()).isEqualTo("financeiro-web");
        assertThat(s.apiAudience()).isEqualTo("financeiro-api");
        assertThat(s.allRoles()).containsExactly("admin", "gestor", "usuario", "auditor");
    }

    @Test
    void nomesReservadosEInvalidosSaoRecusados() {
        assertThatThrownBy(() -> new SystemSlug("master")).isInstanceOf(InvalidSpecException.class);
        assertThatThrownBy(() -> new SystemSlug("plataforma")).isInstanceOf(InvalidSpecException.class);
        assertThatThrownBy(() -> new SystemSlug("Financeiro")).isInstanceOf(InvalidSpecException.class);
        assertThatThrownBy(() -> new SystemSlug("cliente-idp")).isInstanceOf(InvalidSpecException.class);
    }

    @Test
    void urlDoFrontPrecisaSerHttpsOuLocalhost() {
        assertThatThrownBy(() -> new SystemSpec(new SystemSlug("abc"), "A", URI.create("http://app.empresa.com"), List.of(), true))
                .isInstanceOf(InvalidSpecException.class);
        assertThatThrownBy(() -> new SystemSpec(new SystemSlug("abc"), "A", URI.create("https://app.empresa.com/caminho"), List.of(), true))
                .isInstanceOf(InvalidSpecException.class);
        assertThat(new SystemSpec(new SystemSlug("abc"), "A", URI.create("http://localhost:3100/"), List.of(), true).webBaseUrl())
                .hasToString("http://localhost:3100");
    }

    @Test
    void roleDoPadraoNaoPodeSerRepetida() {
        assertThatThrownBy(() -> new SystemSpec(new SystemSlug("abc"), "A", URI.create("https://a.com"), List.of("admin"), true))
                .isInstanceOf(InvalidSpecException.class);
    }

    @Test
    void clienteSoEmSistemaMultiTenant() {
        assertThatThrownBy(() -> TenantSpec.assertAllowedIn(financeiro(false))).isInstanceOf(InvalidSpecException.class);
        TenantSpec.assertAllowedIn(financeiro(true));
    }

    @Test
    void clienteValidaDominiosEAlias() {
        TenantSpec t = new TenantSpec(new TenantAlias("initech"), "Initech", List.of(" Initech.test "), null);
        assertThat(t.domains()).containsExactly("initech.test");
        assertThat(t.loginMethod()).isInstanceOf(LoginMethod.LocalUsers.class);
        assertThat(t.alias().identityProviderAlias()).isEqualTo("initech-idp");
        assertThatThrownBy(() -> new TenantSpec(new TenantAlias("x"), "X", List.of("sem-ponto"), null)).isInstanceOf(InvalidSpecException.class);
        assertThatThrownBy(() -> new TenantAlias("Com Espaco")).isInstanceOf(InvalidSpecException.class);
    }

    @Test
    void envFileTrazOQueOProjetoPrecisa() {
        String env = EnvFile.forSystem(financeiro(true), "https://auth.empresa.com").render("teste");
        assertThat(env)
                .contains("KEYCLOAK_REALM=financeiro")
                .contains("KEYCLOAK_CLIENT_ID=financeiro-web")
                .contains("KEYCLOAK_SCOPE=openid profile email organization")
                .contains("KEYCLOAK_ISSUER=https://auth.empresa.com/realms/financeiro")
                .contains("API_AUDIENCE=financeiro-api")
                .contains("CORS_ALLOWED_ORIGINS=https://financeiro.empresa.com")
                .doesNotContainIgnoringCase("secret");
    }
}
