package br.kerberos.demo.me.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.kerberos.demo.me.application.port.out.CurrentUserPort;
import br.kerberos.demo.me.application.usecase.GetCurrentUserProfileService;
import br.kerberos.demo.me.application.usecase.GetTenantDashboardService;
import br.kerberos.demo.me.domain.CurrentUser;
import br.kerberos.demo.me.domain.TenantAccessDeniedException;
import br.kerberos.demo.me.domain.TenantId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Casos de uso testados com um fake em memória da porta de saída — sem Spring, sem token. */
class UseCasesTest {

    private static CurrentUserPort fakeUser(CurrentUser user) {
        return () -> Optional.ofNullable(user);
    }

    private final CurrentUser ana =
            new CurrentUser("u-ana", "ana", "ana@acme.test", List.of(new TenantId("acme")), Set.of("admin", "usuario"));

    @Test
    void perfilTrazTenantsERoles() {
        var profile = new GetCurrentUserProfileService(fakeUser(ana)).execute();

        assertThat(profile.username()).isEqualTo("ana");
        assertThat(profile.tenants()).containsExactly("acme");
        assertThat(profile.roles()).contains("admin");
    }

    @Test
    void semUsuarioAutenticadoFalha() {
        assertThatThrownBy(() -> new GetCurrentUserProfileService(fakeUser(null)).execute())
                .isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    void dashboardDoProprioTenant() {
        var dashboard = new GetTenantDashboardService(fakeUser(ana)).execute("acme");

        assertThat(dashboard.tenant()).isEqualTo("acme");
        assertThat(dashboard.canManage()).isTrue();
    }

    @Test
    void dashboardDeOutroTenantENegado() {
        assertThatThrownBy(() -> new GetTenantDashboardService(fakeUser(ana)).execute("globex"))
                .isInstanceOf(TenantAccessDeniedException.class);
    }
}
